package net.creeperhost.creeperlauncher.instance.cloud;

import com.google.common.collect.*;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.util.HashUtils;
import net.covers1624.quack.util.LazyValue;
import net.covers1624.quack.util.SneakyUtils;
import net.creeperhost.creeperlauncher.install.tasks.ParallelTaskHelper;
import net.creeperhost.creeperlauncher.install.tasks.Task;
import net.creeperhost.creeperlauncher.pack.Instance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static net.covers1624.quack.util.SneakyUtils.sneak;
import static net.creeperhost.creeperlauncher.instance.cloud.CloudSaveManager.HASH_METADATA;
import static net.creeperhost.creeperlauncher.instance.cloud.CloudSaveManager.LAST_MODIFIED_METADATA;

/**
 * Created by covers1624 on 15/3/23.
 */
public class CloudSyncOperation {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new Gson();

    private final CloudSaveManager saveManager;
    private final Instance instance;

    private List<FileOperation> deleteOperations = List.of();
    private List<FileOperation> uploadOperations = List.of();
    private List<FileOperation> downloadOperations = List.of();

    public CloudSyncOperation(CloudSaveManager saveManager, Instance instance) {
        this.saveManager = saveManager;
        this.instance = instance;
    }

    // TODO throw something other than IOException to capture S3 errors too.
    public void prepare() throws IOException {
        Map<String, S3Object> s3ObjectIndex = saveManager.listInstance(instance);

        LOGGER.info("Checking for remote manifest..");
        SyncManifest remoteManifest = null;
        {
            S3Object obj = s3ObjectIndex.get("sync_manifest.json");
            if (obj != null) {
                LOGGER.info(" Found remote manifest.");
                byte[] bytes = saveManager.downloadToBytes(obj);
                remoteManifest = JsonUtils.parse(GSON, new ByteArrayInputStream(bytes), SyncManifest.class);
            }
        }
        LOGGER.info("Checking for local manifest..");
        SyncManifest localManifest = null;
        {
            Path file = instance.getDir().resolve("sync_manifest.json");
            if (Files.exists(file)) {
                LOGGER.info(" Found local manifest.");
                localManifest = JsonUtils.parse(GSON, file, SyncManifest.class);
            }
        }

//        long currTime = System.currentTimeMillis();

        boolean upload;
        if (remoteManifest == null) {
            upload = true;
        } else if (localManifest == null) {
            upload = false;
        } else {
            if (localManifest.lastSync == remoteManifest.lastSync) {
                upload = true; // TODO check for local time runout?
            } else {
                // TODO not correct
                upload = localManifest.lastSync < remoteManifest.lastSync;
            }
        }
        LOGGER.info("Detected sync direction: {}", upload ? "Upload" : "Download");

        // TODO remove sync_manifest.json from these indexes.
        Map<String, LocalFile> instanceFiles = indexInstance();
        Map<String, RemoteFile> cloudFiles = indexCloud(s3ObjectIndex);

        Set<String> missingRemote = ImmutableSet.copyOf(Sets.difference(instanceFiles.keySet(), cloudFiles.keySet()));
        LOGGER.info("Missing remote files: {}", missingRemote);
        Set<String> missingLocal = ImmutableSet.copyOf(Sets.difference(cloudFiles.keySet(), instanceFiles.keySet()));
        LOGGER.info("Missing local files: {}", missingLocal);
        Set<String> nonMatching = FastStream.of(Sets.intersection(instanceFiles.keySet(), cloudFiles.keySet()))
                .filterNot(e -> instanceFiles.get(e).matches(cloudFiles.get(e)))
                .toSet();
        LOGGER.info("Non-matching files: {}", nonMatching);

        ImmutableList.Builder<FileOperation> deletes = ImmutableList.builder();
        ImmutableList.Builder<FileOperation> uploads = ImmutableList.builder();
        ImmutableList.Builder<FileOperation> downloads = ImmutableList.builder();
        if (upload) {
            for (String s : missingRemote) {
                uploads.add(new FileOperation(OperationKind.UPLOAD, instanceFiles.get(s), null));
            }
            for (String s : missingLocal) {
                deletes.add(new FileOperation(OperationKind.DELETE, null, cloudFiles.get(s)));
            }
            for (String s : nonMatching) {
                uploads.add(new FileOperation(OperationKind.UPLOAD, instanceFiles.get(s), cloudFiles.get(s)));
            }
        } else {
            for (String s : missingRemote) {
                deletes.add(new FileOperation(OperationKind.DELETE, instanceFiles.get(s), null));
            }
            for (String s : missingLocal) {
                downloads.add(new FileOperation(OperationKind.DOWNLOAD, null, cloudFiles.get(s)));
            }
            for (String s : nonMatching) {
                downloads.add(new FileOperation(OperationKind.DOWNLOAD, instanceFiles.get(s), cloudFiles.get(s)));
            }
        }
        deleteOperations = deletes.build();
        uploadOperations = uploads.build();
        downloadOperations = downloads.build();

        LOGGER.info("Operations:");
        for (FileOperation fileOperation : Iterables.concat(deleteOperations, uploadOperations, downloadOperations)) {
            LOGGER.info(" {}", fileOperation.kind);
            if (fileOperation.local != null) {
                LOGGER.info("  {}", fileOperation.local.path);
                if (fileOperation.remote != null) {
                    LOGGER.info("   Len   : {}", fileOperation.local.size());
                    LOGGER.info("   Mod   : {}", fileOperation.local.lastModified());
                    LOGGER.info("   SHA256: {}", fileOperation.local.hash());
                }
            }
            if (fileOperation.remote != null) {
                LOGGER.info("  {}", fileOperation.remote.s3Object);
                if (fileOperation.local != null) {
                    LOGGER.info("   Len   : {}", fileOperation.remote.size());
                    LOGGER.info("   Mod   : {}", fileOperation.remote.lastModified());
                    LOGGER.info("   SHA256: {}", fileOperation.remote.hash());
                }
            }
        }
    }

    public boolean isInSync() {
        return deleteOperations.isEmpty()
                && uploadOperations.isEmpty()
                && downloadOperations.isEmpty();
    }

    public void operate() throws IOException {
        if (isInSync()) {
            LOGGER.info("Already up-to-date.");
            return;
        }

        LOGGER.info("Processing sync..");

        LOGGER.info("Updating sync manifest..");
        SyncManifest manifest = new SyncManifest();
        Path syncManifestFile = instance.getDir().resolve("sync_manifest.json");
        manifest.state = SyncManifest.State.SYNCING;
        manifest.lastSync = System.currentTimeMillis();
        try {
            JsonUtils.write(GSON, syncManifestFile, manifest);
        } catch (IOException ex) {
            LOGGER.error("Failed to update local sync manifest before sync", ex);
            throw ex;
        }
        Throwable syncError = null;
        try {
            saveManager.uploadFile(syncManifestFile, instance.getUuid() + "/sync_manifest.json");
        } catch (Throwable ex) {
            LOGGER.error("Failed to update remote sync manifest before sync.");
            syncError = ex;
        }

        if (syncError == null) {
            try {
                List<Task<Void>> tasks = new LinkedList<>();
                tasks.add((cancelToken, listener) -> {
                    // Delete all local files.
                    for (FileOperation op : deleteOperations) {
                        if (op.local != null) {
                            LOGGER.info("Deleting local file {}", op.local.path);
                            Files.delete(op.local.path);
                        }
                    }
                    // Delete all remote files.
                    saveManager.deleteObjects(FastStream.of(deleteOperations)
                            .filter(e -> e.remote != null)
                            .map(e -> e.remote.s3Object).toList()
                    );
                });
                for (FileOperation op : uploadOperations) {
                    tasks.add((cancelToken, listener) -> {
                        assert op.local != null;
                        LOGGER.info("Uploading file to S3: {}", op.local.path);
                        if (op.remote != null) {
                            saveManager.uploadFile(op.local.path, op.remote.s3Object.key());
                        } else {
                            String key = instance.getUuid() + "/" + instance.getDir().relativize(op.local.path);
                            saveManager.uploadFile(op.local.path, key);
                        }
                    });
                }
                for (FileOperation op : downloadOperations) {
                    tasks.add((cancelToken, listener) -> {
                        assert op.remote != null;
                        LOGGER.info("Downloading file from S3: {}", op.remote.path);
                        if (op.local != null) {
                            saveManager.downloadFile(op.local.path, op.remote.s3Object);
                        } else {
                            Path path = instance.getDir().resolve(op.remote.path);
                            saveManager.downloadFile(path, op.remote.s3Object);
                        }
                    });
                }
                ParallelTaskHelper.executeInParallel(null, Task.TASK_POOL, tasks, null);
            } catch (Throwable ex) {
                LOGGER.error("Failed to process sync, instance may be in an invalid state.", ex);
                syncError = ex;
            }
        }
        boolean jsonUpdated = false;
        try {
            manifest.lastSync = System.currentTimeMillis();
            manifest.state = syncError == null ? SyncManifest.State.SYNCED : SyncManifest.State.UNFINISHED;
            JsonUtils.write(GSON, syncManifestFile, manifest);
            jsonUpdated = true;
        } catch (IOException ex) {
            LOGGER.error("Failed to update local sync_manifest after sync.", ex);
            if (syncError != null) {
                syncError.addSuppressed(ex);
            } else {
                syncError = ex;
            }
        }

        if (jsonUpdated) {
            try {
                saveManager.uploadFile(syncManifestFile, instance.getUuid() + "/sync_manifest.json");
            } catch (Throwable ex) {
                LOGGER.error("Failed to upload sync manifest after sync.", ex);
                if (syncError != null) {
                    syncError.addSuppressed(ex);
                } else {
                    syncError = ex;
                }
            }
        }
        if (syncError != null) {
            LOGGER.error("Sync failed with error: ", syncError);
            SneakyUtils.throwUnchecked(syncError);
        }
        LOGGER.info("Cloud sync finished!");
    }

    private Map<String, LocalFile> indexInstance() throws IOException {
        ImmutableMap.Builder<String, LocalFile> builder = ImmutableMap.builder();
        Path rootDir = instance.getDir();
        try (Stream<Path> stream = Files.walk(rootDir)) {
            stream.forEach(e -> {
                if (!Files.isDirectory(e)) {
                    LocalFile path = new LocalFile(e);
                    builder.put(path.path(), path);
                }
            });
        }
        return builder.build();

    }

    private Map<String, RemoteFile> indexCloud(Map<String, S3Object> s3ObjectIndex) {
        ImmutableMap.Builder<String, RemoteFile> builder = ImmutableMap.builder();
        for (S3Object s3Object : s3ObjectIndex.values()) {
            CompletableFuture<Map<String, String>> metadataFuture = CompletableFuture.supplyAsync(() -> saveManager.getMetadata(s3Object), Task.TASK_POOL);
            RemoteFile path = new RemoteFile(s3Object, metadataFuture);
            builder.put(path.path(), path);
        }
        return builder.build();
    }

    public record FileOperation(OperationKind kind, @Nullable LocalFile local, @Nullable RemoteFile remote) {
    }

    public enum OperationKind {
        DELETE,
        UPLOAD,
        DOWNLOAD,
    }

    private abstract static class IndexedFile {

        public abstract String path();

        public abstract long size();

        @Nullable
        public abstract Instant lastModified();

        @Nullable
        public abstract HashCode hash();

        public boolean matches(IndexedFile other) {
            if (size() != other.size()) return false;

            Instant l1 = lastModified();
            Instant l2 = other.lastModified();
            if (l1 == null || l2 == null) return false;
            if (l1.toEpochMilli() != l2.toEpochMilli()) return false;

            HashCode h1 = hash();
            HashCode h2 = other.hash();
            if (h1 == null || h2 == null) return false;
            return h1.equals(h2);
        }
    }

    @SuppressWarnings ("UnstableApiUsage")
    private class LocalFile extends IndexedFile {

        private final Path path;
        private final String pathStr;
        private final LazyValue<HashCode> hash;

        private LocalFile(Path path) {
            this.path = path;
            pathStr = instance.path.relativize(path).toString();
            hash = new LazyValue<>(sneak(() -> HashUtils.hash(Hashing.sha256(), path)));
        }

        @Override
        public String path() {
            return pathStr;
        }

        @Override
        public long size() {
            try {
                return Files.size(path);
            } catch (IOException ex) {
                return 0;
            }
        }

        @Override
        public Instant lastModified() {
            try {
                return Files.getLastModifiedTime(path).toInstant();
            } catch (IOException e) {
                return Instant.EPOCH;
            }
        }

        @Override
        public HashCode hash() {
            return hash.get();
        }
    }

    private class RemoteFile extends IndexedFile {

        private final S3Object s3Object;
        private final String path;
        private final LazyValue<Map<String, String>> metadata;

        private RemoteFile(S3Object s3Object, CompletableFuture<Map<String, String>> metadataFuture) {
            this.s3Object = s3Object;
            path = s3Object.key().replace(instance.getUuid().toString() + "/", "");
            metadata = new LazyValue<>(() -> {
                try {
                    return metadataFuture.get();
                } catch (InterruptedException | ExecutionException ex) {
                    throw new RuntimeException("Failed to wait for metadata task.", ex);
                }
            });
        }

        @Override
        public String path() {
            return path;
        }

        @Override
        public long size() {
            return s3Object.size();
        }

        @Nullable
        @Override
        public Instant lastModified() {
            String lastModified = metadata.get().get(LAST_MODIFIED_METADATA);
            if (lastModified == null) return null;

            try {
                return Instant.ofEpochMilli(Long.parseLong(lastModified));
            } catch (NumberFormatException ex) {
                // TODO log or just propagate exception?
                return null;
            }
        }

        @Nullable
        @Override
        public HashCode hash() {
            String hash = metadata.get().get(HASH_METADATA);
            if (hash == null) return null;

            return HashCode.fromString(hash);
        }
    }
}
