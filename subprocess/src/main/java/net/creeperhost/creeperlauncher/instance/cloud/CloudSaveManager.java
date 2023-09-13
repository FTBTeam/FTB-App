package net.creeperhost.creeperlauncher.instance.cloud;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
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
import net.creeperhost.creeperlauncher.api.data.instances.CloudSavesReloadedData;
import net.creeperhost.creeperlauncher.api.data.instances.InstanceCloudSyncConflictData;
import net.creeperhost.creeperlauncher.api.handlers.instances.InstalledInstancesHandler;
import net.creeperhost.creeperlauncher.data.InstanceJson;
import net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest;
import net.creeperhost.creeperlauncher.install.OperationProgressTracker;
import net.creeperhost.creeperlauncher.instance.cloud.CloudSaveManager.SyncResult.ResultType;
import net.creeperhost.creeperlauncher.instance.cloud.CloudSyncOperation.SyncDirection;
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
import software.amazon.awssdk.services.s3.S3Configuration;
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

    private final Map<UUID, SyncEntry> syncOperations = new HashMap<>();
    private final Map<UUID, CompletableFuture<Void>> removeOperations = new HashMap<>();

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
                    .endpointOverride(URI.create(s3Host))
                    .region(Region.US_WEST_1)
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build()) // Enable path-style addressing
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(s3Key, s3Secret)))
                    .httpClient(new OkHTTPS3HttpClient(Constants::httpClient));
            s3Client = builder.build();
            LOGGER.info("Cloud saves configured!");
        } catch (Throwable ex) {
            LOGGER.error("Failed to configure Cloud saves.", ex);
        }
    }

    public CompletableFuture<SyncResult> requestInstanceSync(Instance instance) {
        return _requestInstanceSync(instance, false);
    }

    public CompletableFuture<SyncResult> requestInitialSync(Instance instance) {
        return _requestInstanceSync(instance, true);
    }

    private CompletableFuture<SyncResult> _requestInstanceSync(Instance instance, boolean initialSync) {
        SyncEntry entry = syncOperations.get(instance.getUuid());

        if (entry != null) {
            if (entry.isInitialSync) return CompletableFuture.completedFuture(new SyncResult(ResultType.INITIAL_STILL_RUNNING, "Initial sync is still running."));
            throw new IllegalStateException("Sync already requested.");
        }

        synchronized (syncOperations) {
            entry = new SyncEntry(new CloudSyncOperation(this, instance), initialSync);
            syncOperations.put(instance.getUuid(), entry);
            return submitSync(entry);
        }
    }

    public @Nullable CompletableFuture<Void> requestDisableSync(Instance instance) {
        LOGGER.info("Requesting disabling sync for {}({})", instance.getName(), instance.getUuid());
        SyncEntry entry = syncOperations.get(instance.getUuid());
        if (entry != null) {
            return null;
        }
        synchronized (removeOperations) {
            CompletableFuture<Void> future = removeOperations.get(instance.getUuid());
            if (future != null && !future.isDone()) {
                // TODO, this will only ever be hit if the user presses the disable button multiple times without
                //       any locks on the UI side.
                //       It would be more correct to return the existing future, but ehh.
                return null;
            }
            future = CompletableFuture.runAsync(() -> {
                Map<String, S3Object> files = listInstance(instance);
                LOGGER.info(" Deleting {} files.", files.size());
                deleteObjects(files.values());
                try {
                    Files.deleteIfExists(instance.path.resolve("sync_manifest.json"));
                } catch (IOException ex) {
                    LOGGER.error("Failed to delete sync_manifest.json", ex);
                }
            }, EXECUTOR);

            future = future.thenRunAsync(() -> {
                synchronized (removeOperations) {
                    removeOperations.remove(instance.getUuid());
                }
                LOGGER.info("Sync disabled! {}({})", instance.getName(), instance.getUuid());
            });
            removeOperations.put(instance.getUuid(), future);
            return future;
        }
    }

    public void resolveConflict(UUID instance, SyncDirection resolution) {
        SyncEntry entry = syncOperations.get(instance);
        if (entry == null) throw new IllegalStateException("Instance is not pending sync.");
        if (entry.conflict != null) throw new IllegalStateException("Instance sync is not in conflict?");
        entry.resolution = resolution;
        submitSync(entry);
    }

    private CompletableFuture<SyncResult> submitSync(SyncEntry entry) {
        CompletableFuture<SyncResult> future = CompletableFuture.supplyAsync(() -> {
            SyncResult result = entry.run();
            onSyncFinished(entry.operation.instance, result);
            return result;
        }, EXECUTOR);
        entry.future = future;
        return future;
    }

    private void onSyncFinished(Instance instance, SyncResult result) {
        if (result.type != ResultType.CONFLICT) {
            synchronized (syncOperations) {
                syncOperations.remove(instance.getUuid());
            }
        }
    }

    public boolean isCloudPollInProgress() {
        return pollFuture != null && !pollFuture.isDone();
    }

    public CompletableFuture<Void> pollCloudInstances() {
        if (pollFuture != null && !pollFuture.isDone()) return pollFuture;

        if (!isConfigured()) {
            LOGGER.info("Skipping loading cloud saves. Not configured.");
            return CompletableFuture.failedFuture(new Throwable("Cloud saves not configured."));
        }

        return pollFuture = CompletableFuture.runAsync(() -> {
            OperationProgressTracker tracker = new OperationProgressTracker("cloud_poll", Map.of());
            try {
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
                List<UUID> removedPending = FastStream.of(Instances.allInstances())
                        .filter(Instance::isPendingCloudInstance)
                        .map(Instance::getUuid)
                        .filterNot(e -> keys.contains(e.toString()))
                        .toList();
                for (Instance instance : Instances.allInstances()) {
                    // Remove any synced instances.
                    keys.remove(instance.getUuid().toString());
                }
                List<Instance> newInstances = new ArrayList<>();
                // Make sure the directories the un synced cloud instances would use, don't exist, or are empty.
                Path instancesDir = Settings.getInstancesDir();
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
                            LOGGER.warn("Error loading cloud save list, {} exist and is not empty. This could save cannot be loaded.", instanceDir);
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
                        InstanceJson instanceManifest = InstanceJson.load(downloadToBytes(manifest));
                        ModpackVersionManifest versionManifest = JsonUtils.parse(ModpackVersionManifest.GSON, new ByteArrayInputStream(downloadToBytes(version)), ModpackVersionManifest.class);
                        LOGGER.info("Loaded pending cloud instance {}.", key);
                        Instance instance = new Instance(instanceDir, instanceManifest, versionManifest);
                        Instances.addInstance(instance);
                        newInstances.add(instance);
                    } catch (IOException ex) {
                        LOGGER.warn("Failed to load pending cloud instance {}.", key, ex);
                    }
                }
                List<InstanceJson> instanceJsons = new ArrayList<>();
                if (!newInstances.isEmpty()) {
                    for (Instance instance : newInstances) {
                        instanceJsons.add(new InstalledInstancesHandler.SugaredInstanceJson(instance.props, instance.path, true));
                    }
                }
                for (Instance instance : Instances.allInstances()) {
                    // The instance did have cloud-saves enabled, but its files don't exist in the S3 bucket anymore
                    // this likely means that the user has disabled cloud saves on another machine. We need to disable
                    // it here as well.
                    if (instance.props.cloudSaves && !keys.contains(instance.getUuid().toString())) {
                        try {
                            instance.props.cloudSaves = false;
                            instance.saveJson();
                            Files.deleteIfExists(instance.path.resolve("sync_manifest.json"));
                        } catch (IOException ex) {
                            LOGGER.error("Failed to disable cloud saves for instance which has had its remote files deleted.", ex);
                        }
                        instanceJsons.add(new InstalledInstancesHandler.SugaredInstanceJson(instance.props, instance.path, false));
                    }
                }
                if (!instanceJsons.isEmpty() || !removedPending.isEmpty()) {
                    Settings.webSocketAPI.sendMessage(new CloudSavesReloadedData(instanceJsons, removedPending));
                }
            } finally {
                tracker.finished();
            }
        }, EXECUTOR);
    }

    public boolean isSyncing(UUID uuid) {
        return syncOperations.containsKey(uuid);
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

        ImmutableList.Builder<S3Object> objects = ImmutableList.builder();
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .prefix(prefix)
                .bucket(s3Bucket)
                .build();
        for (ListObjectsV2Response response : s3Client.listObjectsV2Paginator(request)) {
            List<S3Object> toAdd = response.contents();
            if (DEBUG) {
                LOGGER.info(" Adding {} objects to list.", toAdd.size());
            }
            objects.addAll(toAdd);
        }

        List<S3Object> built = objects.build();
        if (DEBUG) {
            LOGGER.info("Finished listing bucket. {} objects.", built.size());
        }
        return built;
    }

    public void deleteObjects(Collection<S3Object> objects) {
        assert s3Client != null;
        if (objects.isEmpty()) return;

        List<ObjectIdentifier> files = FastStream.of(objects)
                .map(e -> ObjectIdentifier.builder().key(e.key()).build())
                .toList();

        // S3 docs say that only 1000 objects can be deleted at a time.
        List<List<ObjectIdentifier>> partitions = Lists.partition(files, 1000);

        LOGGER.info("Deleting {} objects in {} parts", objects.size(), partitions.size());

        for (List<ObjectIdentifier> partitioned : partitions) {
            if (DEBUG) {
                LOGGER.info("Deleting: ");
                for (ObjectIdentifier object : partitioned) {
                    LOGGER.info(" " + object.key());
                }
            }
            s3Client.deleteObjects(request -> {
                request.bucket(s3Bucket);
                request.delete(del -> {
                    del.objects(partitioned);
                });
            });

            LOGGER.info("Deleted part.");
        }
        LOGGER.info("Finished deleting.");
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

    public record SyncResult(ResultType type, String reason) {

        public enum ResultType {
            CONFLICT,
            SUCCESS,
            FAILED,
            INITIAL_STILL_RUNNING,
        }
    }

    public static final class SyncEntry {

        public final UUID uuid;
        public final CloudSyncOperation operation;
        public final boolean isInitialSync;
        @Nullable
        public Future<?> future;

        public @Nullable CloudSyncOperation.ConflictException conflict;
        public @Nullable SyncDirection resolution;
        public boolean complete = false;

        public SyncEntry(CloudSyncOperation operation, boolean isInitialSync) {
            this.uuid = operation.instance.getUuid();
            this.operation = operation;
            this.isInitialSync = isInitialSync;
        }

        public SyncResult run() {
            try {
                operation.prepare(resolution);
            } catch (CloudSyncOperation.ConflictException ex) {
                conflict = ex;
                Settings.webSocketAPI.sendMessage(new InstanceCloudSyncConflictData(uuid, ex.code, ex.message));
                return new SyncResult(ResultType.CONFLICT, "Sync conflict. Requires resolution.");
            } catch (Throwable ex) {
                LOGGER.error("Fatal error preparing instance for sync.", ex);
                complete = true;
                return new SyncResult(ResultType.FAILED, "Failed to prepare instance for sync. See logs.");
            }

            try {
                operation.operate();
                return new SyncResult(ResultType.SUCCESS, "Synced!");
            } catch (Throwable ex) {
                LOGGER.error("Failed to sync instance.", ex);
                complete = true;
                return new SyncResult(ResultType.FAILED, "Sync failed. See logs.");
            }
        }
    }
}
