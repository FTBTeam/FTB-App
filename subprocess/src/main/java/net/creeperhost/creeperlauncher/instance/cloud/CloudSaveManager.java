package net.creeperhost.creeperlauncher.instance.cloud;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonObject;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.io.IOUtils;
import net.covers1624.quack.util.HashUtils;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.data.InstanceJson;
import net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest;
import net.creeperhost.creeperlauncher.pack.Instance;
import net.creeperhost.creeperlauncher.util.s3.OkHTTPS3HttpClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.Header;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Created by covers1624 on 23/11/22.
 */
@SuppressWarnings ("UnstableApiUsage")
public final class CloudSaveManager {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final boolean DEBUG = Boolean.getBoolean("CloudSaveManager.debug");

    // Match uuid at either the start of the line, or prefixed with a slash.
    private static final Pattern INSTANCE_UUID_REGEX = Pattern.compile("(?>^/|^)([0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12})/");

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("Cloud Save Manager").setDaemon(true).build());

    public static final String HASH_METADATA = "x-sha256";
    public static final String LAST_MODIFIED_METADATA = "x-last-modified";
    public static final String REAL_NAME_METADATA = "x-real-name";

    @Nullable
    private String s3Host;
    @Nullable
    private String s3Bucket;
    @Nullable
    private String s3Key;
    @Nullable
    private String s3Secret;

    @Nullable
    private S3Client s3Client;

    private final List<SyncEntry> syncOperations = new ArrayList<>();

    @Nullable
    private CompletableFuture<Void> pollFuture;

    public CloudSaveManager() {
        String sysProp = System.getProperty("CloudSaveManager.creds");
        if (sysProp != null) {
            Path path = Path.of(sysProp);
            if (Files.exists(path)) {
                try {
                    JsonObject obj = JsonUtils.parseRaw(path).getAsJsonObject();
                    LOGGER.info("Configuring cloud saves from SystemProperty override.");
                    configure(
                            JsonUtils.getString(obj, "s3Host"),
                            JsonUtils.getString(obj, "s3Bucket"),
                            JsonUtils.getString(obj, "s3Key"),
                            JsonUtils.getString(obj, "s3Secret")
                    );
                } catch (IOException ex) {
                    LOGGER.error("Failed to configure dev creds.");
                }
            }
        }
    }

    public void configure(String s3Host, String s3Bucket, String s3Key, String s3Secret) {
        if (s3Client != null) {
            LOGGER.info("Reconfiguring cloud saves...");
            if (Objects.equals(this.s3Host, s3Host)
                    && Objects.equals(this.s3Bucket, s3Bucket)
                    && Objects.equals(this.s3Key, s3Key)
                    && Objects.equals(this.s3Secret, s3Secret)) {
                LOGGER.info("Skipping reconfigure, details are identical.");
                return;
            }

            // TODO We may need to delay this if we end up taking too long waiting for syncs to finish, etc.
            close();
        }
        assert s3Client == null;

        if (isEmpty(s3Host) || isEmpty(s3Bucket) || isEmpty(s3Key) || isEmpty(s3Secret)) {
            return;
        }

        this.s3Host = s3Host;
        this.s3Bucket = s3Bucket;
        this.s3Key = s3Key;
        this.s3Secret = s3Secret;

        LOGGER.info("Configuring cloud saves..");

        try {
            S3ClientBuilder builder = S3Client.builder()
                    .endpointOverride(URI.create("http://%s:8080".formatted(s3Host)))
                    .region(Region.US_WEST_1)
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(s3Key, s3Secret)))
                    .httpClient(new OkHTTPS3HttpClient(Constants::httpClient));
            s3Client = builder.build();
            LOGGER.info("Cloud saves configured!");
        } catch (Throwable ex) {
            LOGGER.error("Failed to configure Cloud saves.", ex);
        }
    }

    public void requestInstanceSync(Instance instance) {
        synchronized (syncOperations) {
            SyncEntry entry = new SyncEntry(new CloudSyncOperation(this, instance));
            syncOperations.add(entry);
            entry.future = EXECUTOR.submit(() -> {
                try {
                    entry.operation.prepare();
                    entry.operation.operate();
                } catch (Throwable ex) {
                    // TODO push to UI.
                    LOGGER.error("Cloud sync failed!", ex);
                } finally {
                    synchronized (syncOperations) {
                        syncOperations.remove(entry);
                    }
                }
            });
        }
    }

    public boolean isCloudPollInProgress() {
        return pollFuture != null && !pollFuture.isDone();
    }

    public CompletableFuture<Void> pollCloudInstances() {
        if (isCloudPollInProgress()) return pollFuture;

        if (!isConfigured()) {
            LOGGER.info("Skipping loading cloud saves. Not configured.");
            return CompletableFuture.failedFuture(new Throwable("Cloud saves not configured."));
        }

        return CompletableFuture.runAsync(() -> {
            // TODO poke UI we are polling for cloud saves.
            Set<String> keys = new HashSet<>();
            List<S3Object> index;
            try {
                Matcher matcher = INSTANCE_UUID_REGEX.matcher("");
                index = listBucket("");
                for (S3Object s3Object : index) {
                    matcher.reset(s3Object.key());
                    if (!matcher.find()) continue;
                    String uuid = matcher.group(1);
                    keys.add(uuid);
                }
            } catch (Throwable ex) {
                LOGGER.warn("Failed to list bucket.", ex);
                return;
            }
            for (Instance instance : Instances.allInstances()) {
                // Remove any synced instances.
                keys.remove(instance.getUuid().toString());
            }
            // Make sure the directories the un synced cloud instances would use, don't exist, or are empty.
            Path instancesDir = Settings.getInstancesDir();
            boolean loaded = false;
            for (String key : keys) {
                Path instanceDir = instancesDir.resolve(key);
                if (Files.notExists(instanceDir)) continue;
                if (!Files.isDirectory(instanceDir)) {
                    // TODO UI warning for this.
                    LOGGER.warn("Error loading cloud save list, {} exists, but is not a directory. This cloud save cannot be loaded.", instanceDir);
                    continue;
                }
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(instanceDir)) {
                    if (stream.iterator().hasNext()) {
                        // TODO UI warning for this.
                        LOGGER.warn("Error loading cloud save list, {} exist and is not empty. THis could save cannot be loaded.", instanceDir);
                        continue;
                    }
                } catch (IOException ex) {
                    // TODO UI warning for this.
                    LOGGER.warn("Error loading cloud save list. Unable to iterate directory {}. This cloud save cannot be loaded.", instanceDir);
                    continue;
                }

                Map<String, S3Object> objects = FastStream.of(index)
                        .filter(e -> e.key().startsWith(key))
                        .toMap(e -> StringUtils.stripStart(key + "/", e.key()), e -> e);

                S3Object manifest = objects.get("instance.json");
                if (manifest == null) {
                    LOGGER.warn("Error loading cloud save list. Cloud instance {} is missing instance.json file. Loading impossible.", key);
                    continue;
                }

                S3Object version = objects.get("version.json");
                if (version == null) {
                    LOGGER.warn("Error loading cloud save list. Cloud instance {} is missing version.json file. Loading impossible.", key);
                    continue;
                }

                try {
                    InstanceJson instanceManifest =  InstanceJson.load(downloadToBytes(manifest));
                    ModpackVersionManifest versionManifest = JsonUtils.parse(ModpackVersionManifest.GSON, new ByteArrayInputStream(downloadToBytes(version)), ModpackVersionManifest.class);
                    LOGGER.info("Loaded pending cloud instance {}.", key);
                    Instances.addInstance(new Instance(instanceDir, instanceManifest, versionManifest));
                    loaded = true;
                } catch (IOException ex) {
                    LOGGER.warn("Failed to load pending cloud instance {}.", key, ex);
                }
            }
            if (loaded) {
                // TODO, poke UI that new instances exist.
            }
        }, EXECUTOR);
    }

    public List<SyncEntry> getSyncOperations() {
        synchronized (syncOperations) {
            return new ArrayList<>(syncOperations);
        }
    }

    public boolean isConfigured() {
        return s3Client != null;
    }

    public void uploadFile(Path file, String destKey) throws IOException {
        assert s3Client != null;
        Map<String, String> metadata = new HashMap<>();
        long len = Files.size(file);
        metadata.put(Header.CONTENT_LENGTH, String.valueOf(len));
        String sha256 = HashUtils.hash(Hashing.sha256(), file).toString();
        metadata.put(HASH_METADATA, sha256);
        metadata.put(LAST_MODIFIED_METADATA, Long.toString(Files.getLastModifiedTime(file).toMillis()));

        PutObjectRequest.Builder builder = PutObjectRequest.builder()
                .bucket(s3Bucket)
                .key(destKey)
                .contentLength(len)
                .contentType(Files.probeContentType(file))
                .checksumSHA256(sha256)
                .metadata(metadata);

        s3Client.putObject(builder.build(), RequestBody.fromFile(file));
    }

    public void downloadFile(Path file, S3Object s3Object) throws IOException {
        assert s3Client != null;

        Path tempFile = file.getParent().resolveSibling("__tmp_" + file.getFileName());
        try (ResponseInputStream<GetObjectResponse> is = s3Client.getObject(e -> e.bucket(s3Bucket).key(s3Object.key()))) {
            GetObjectResponse response = is.response();
            try (OutputStream os = Files.newOutputStream(tempFile)) {
                IOUtils.copy(is, os);
            }
            long actualLen = Files.size(tempFile);
            if (response.contentLength() != actualLen) {
                throw new IOException("File failed length validation. Expected: " + response.contentLength() + " Actual: " + actualLen);
            }

            String expectedHash = null;
            String lastModified = null;
            if (response.hasMetadata()) {
                expectedHash = response.metadata().get(HASH_METADATA);
                lastModified = response.metadata().get(LAST_MODIFIED_METADATA);
            }
            if (expectedHash != null) {
                HashCode actualHash = HashUtils.hash(Hashing.sha1(), tempFile);
                if (HashUtils.equals(actualHash, expectedHash)) {
                    throw new IOException("File failed SHA1 validation. Expected: " + expectedHash + " Actual: " + actualHash);
                }
            }
            Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING);

            if (lastModified != null) {
                try {
                    Files.setLastModifiedTime(file, FileTime.fromMillis(Long.parseLong(lastModified)));
                } catch (NumberFormatException ex) {
                    throw new IOException("x-last-modified metadata was malformed.", ex);
                }
            }
        } finally {
            if (Files.exists(tempFile)) { // This will only exist if validation failed.
                try {
                    Files.delete(tempFile);
                } catch (IOException ex) {
                    LOGGER.error("Failed to cleanup temp file after failure.", ex);
                }
            }
        }
    }

    public byte[] downloadToBytes(S3Object s3Object) throws IOException {
        assert s3Client != null;

        try (ResponseInputStream<GetObjectResponse> is = s3Client.getObject(e -> e.bucket(s3Bucket).key(s3Object.key()))) {
            return IOUtils.toBytes(is);
        }
    }

    public Map<String, S3Object> listInstance(Instance instance) {
        ImmutableMap.Builder<String, S3Object> builder = ImmutableMap.builder();
        String prefix = instance.getUuid() + "/";
        for (S3Object s3Object : listBucket(prefix)) {
            builder.put(StringUtils.removeStart(s3Object.key(), prefix), s3Object);
        }
        return builder.build();
    }

    public List<S3Object> listBucket(String prefix) {
        if (s3Client == null) return List.of();
        if (DEBUG) {
            LOGGER.info("Listing entire bucket with prefix: {}", prefix);
        }

        // TODO, this can in theory use v2, but for some reason the S3 server we poke is on meth and refuses
        //       to give us continuation tokens, thus, we must use v1 object listing.

        ImmutableList.Builder<S3Object> objects = ImmutableList.builder();
        ListObjectsResponse response = null;
        do {
            if (response != null) {
                if (response.nextMarker() == null) {
                    LOGGER.fatal("Received truncated response without a continuation marker.");
                    break;
                }
                if (DEBUG) {
                    LOGGER.info(" Got continuation token..");
                }
            }
            ListObjectsRequest request = ListObjectsRequest.builder()
                    .prefix(prefix)
                    .bucket(s3Bucket)
                    .marker(response != null ? response.nextMarker() : null)
                    .build();
            response = s3Client.listObjects(request);
            List<S3Object> toAdd = response.contents();
            if (DEBUG) {
                LOGGER.info(" Adding {} objects to list.", toAdd.size());
            }
            objects.addAll(toAdd);
        }
        while (response.isTruncated());

        List<S3Object> built = objects.build();
        if (DEBUG) {
            LOGGER.info("Finished listing bucket. {} objects.", built.size());
        }
        return built;
    }

    public void deleteObjects(List<S3Object> objects) {
        assert s3Client != null;
        if (objects.isEmpty()) return;

        if (DEBUG) {
            LOGGER.info("Deleting objects: ");
            for (S3Object object : objects) {
                LOGGER.info(" " + object.key());
            }
        }

        DeleteObjectsResponse response = s3Client.deleteObjects(request -> {
            request.bucket(s3Bucket);
            request.delete(del -> {
                del.objects(FastStream.of(objects)
                        .map(e -> ObjectIdentifier.builder().key(e.key()).build())
                        .toList()
                );
            });
        });
        LOGGER.info(response);
    }

    public Map<String, String> getMetadata(S3Object s3Object) {
        assert s3Client != null;
        return s3Client.headObject(e -> e.bucket(s3Bucket).key(s3Object.key())).metadata();
    }

    public void close() {
        if (s3Client != null) {
            // TODO stop any cloud syncs in progress.
            // Perhaps we will need to 'schedule' the shutdown until syncs have finished.
            s3Client.close();
            s3Client = null;
        }
    }

    public static final class SyncEntry {

        @Nullable
        public Future<?> future;
        public final CloudSyncOperation operation;

        public SyncEntry(CloudSyncOperation operation) {
            this.operation = operation;
        }

    }
}
