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
import net.creeperhost.creeperlauncher.data.InstanceJson;
import net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest;
import net.creeperhost.creeperlauncher.install.OperationProgressTracker;
import net.creeperhost.creeperlauncher.install.tasks.*;
import net.creeperhost.creeperlauncher.install.tasks.modloader.ModLoaderInstallTask;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static net.covers1624.quack.util.SneakyUtils.sneak;
import static net.creeperhost.creeperlauncher.instance.cloud.CloudSaveManager.HASH_METADATA;
import static net.creeperhost.creeperlauncher.instance.cloud.CloudSaveManager.LAST_MODIFIED_METADATA;
import static net.creeperhost.creeperlauncher.instance.cloud.SyncManifest.State.*;

/**
 * Created by covers1624 on 15/3/23.
 */
public class CloudSyncOperation {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new Gson();

    // All files here need to be lower-case.
    private static final List<String> IGNORED_FILES = List.of(
            "sync_manifest.json", // This is an internal file and managed manually.

            "logs/",              // Can get very large if mods spam logs.
            "backups/",           // Backups are just massive. Lets just not..

            // Potentially sensitive information we should avoid syncing.
            // Add more here when discovered!
            ".reauth.cfg",
            ".mtsession/"
    );

    // Prioritize specific files. These will always be first in this order.
    public static final List<String> FILE_PRIORITIES = List.of(
            "saves/",
            "local/",
            "config/",
            "scripts/",
            "kubejs/"
            // Then everything else
    );

    // These files will always upload/download last.
    public static final List<String> FILE_UN_PRIORITIES = List.of(
            "mods/",
            "mods2/" // Why does this exist in ATM7?
    );

    private final CloudSaveManager saveManager;
    public final Instance instance;
    private final OperationProgressTracker progressTracker;

    private SyncDirection direction = SyncDirection.UP_TO_DATE;
    private List<FileOperation> deleteOperations = List.of();
    private List<FileOperation> uploadOperations = List.of();
    private List<FileOperation> downloadOperations = List.of();

    public CloudSyncOperation(CloudSaveManager saveManager, Instance instance) {
        this.saveManager = saveManager;
        this.instance = instance;
        progressTracker = new OperationProgressTracker("sync", Map.of("instance", instance.getUuid().toString()));
    }

    // TODO throw something other than IOException to capture S3 errors too.
    public void prepare(@Nullable SyncDirection conflictResolution) throws IOException, ConflictException {
        progressTracker.nextStage(SyncStage.PREPARE);
        Map<String, S3Object> s3ObjectIndex = saveManager.listInstance(instance);

        LOGGER.info("Checking for remote manifests..");
        SyncManifest remoteManifest = null;
        InstanceJson remoteInstance = null;
        {
            S3Object syncManifest = s3ObjectIndex.get("sync_manifest.json");
            if (syncManifest != null) {
                LOGGER.info(" Found remote manifest.");
                byte[] bytes = saveManager.downloadToBytes(syncManifest);
                remoteManifest = JsonUtils.parse(GSON, new ByteArrayInputStream(bytes), SyncManifest.class);
            }
            S3Object instanceManifest = s3ObjectIndex.get("instance.json");
            if (instanceManifest != null) {
                LOGGER.info(" Found remote instance json.");
                byte[] bytes = saveManager.downloadToBytes(instanceManifest);
                remoteInstance = InstanceJson.load(bytes);
            }
        }
        LOGGER.info("Checking for local manifest..");
        SyncManifest localManifest = null;
        InstanceJson localInstance = instance.props;
        {
            Path file = instance.getDir().resolve("sync_manifest.json");
            if (Files.exists(file)) {
                LOGGER.info(" Found local manifest.");
                localManifest = JsonUtils.parse(GSON, file, SyncManifest.class);
            }
        }

        if (conflictResolution == null) {
            try {
                direction = determineDirection(remoteManifest, remoteInstance, localManifest, localInstance);
            } catch (ConflictException ex) {
                progressTracker.nextStage(SyncStage.CONFLICT);
                throw ex;
            }
        } else {
            direction = conflictResolution;
        }
        // TODO perhaps we should pick a default direction (UPLOAD?) and still do a files check?
        if (direction == SyncDirection.UP_TO_DATE) {
            LOGGER.info("Sync is up-to-date.");
            return;
        }

        LOGGER.info("Detected sync direction: {}", direction);

        progressTracker.nextStage(SyncStage.INDEXING_LOCAL);
        Map<String, LocalFile> instanceFiles = indexInstance();
        progressTracker.nextStage(SyncStage.INDEXING_REMOTE); // TODO, we can update the step progress!
        Map<String, RemoteFile> cloudFiles = indexCloud(s3ObjectIndex);

        progressTracker.nextStage(SyncStage.COMPUTING_CHANGES);
        Set<String> missingRemote = ImmutableSet.copyOf(Sets.difference(instanceFiles.keySet(), cloudFiles.keySet()));
        LOGGER.info("Missing remote files: {}", missingRemote);
        Set<String> missingLocal = ImmutableSet.copyOf(Sets.difference(cloudFiles.keySet(), instanceFiles.keySet()));
        LOGGER.info("Missing local files: {}", missingLocal);
        Set<String> nonMatching = FastStream.of(Sets.intersection(instanceFiles.keySet(), cloudFiles.keySet()))
                .filterNot(e -> instanceFiles.get(e).matches(cloudFiles.get(e)))
                .toSet();
        LOGGER.info("Non-matching files: {}", nonMatching);

        List<FileOperation> deletes = new ArrayList<>();
        List<FileOperation> uploads = new ArrayList<>();
        List<FileOperation> downloads = new ArrayList<>();
        if (direction == SyncDirection.UPLOAD) {
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

        Collections.sort(uploads);
        Collections.sort(downloads);

        deleteOperations = ImmutableList.copyOf(deletes);
        uploadOperations = ImmutableList.copyOf(uploads);
        downloadOperations = ImmutableList.copyOf(downloads);

        LOGGER.info("Operations:");
        for (FileOperation fileOperation : Iterables.concat(deleteOperations, uploadOperations, downloadOperations)) {
            if (fileOperation.local != null) {
                LOGGER.info("  {} {}", fileOperation.kind, fileOperation.local.path());
                if (fileOperation.remote != null) {
                    LOGGER.info("   Len   : {}", fileOperation.local.size());
                    LOGGER.info("   Mod   : {}", fileOperation.local.lastModified());
                    LOGGER.info("   SHA256: {}", fileOperation.local.hash());
                }
            }
            if (fileOperation.remote != null) {
                LOGGER.info("  {} {}", fileOperation.kind, fileOperation.remote.path());
                if (fileOperation.local != null) {
                    LOGGER.info("   Len   : {}", fileOperation.remote.size());
                    LOGGER.info("   Mod   : {}", fileOperation.remote.lastModified());
                    LOGGER.info("   SHA256: {}", fileOperation.remote.hash());
                }
            }
        }
    }

    public boolean isInSync() {
        return direction == SyncDirection.UP_TO_DATE;
    }

    public void operate() throws IOException {
        progressTracker.nextStage(SyncStage.BEGIN_SYNC);
        try {
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
                saveManager.uploadFile(syncManifestFile, instance.getUuid() + "/sync_manifest.json", null);
            } catch (Throwable ex) {
                LOGGER.error("Failed to update remote sync manifest before sync.");
                syncError = ex;
            }

            if (syncError == null) {
                try {
                    long totalSize = 0;
                    List<Task<Void>> tasks = new LinkedList<>();
                    if (direction == SyncDirection.UPLOAD) {
                        assert downloadOperations.isEmpty();
                        progressTracker.nextStage(SyncStage.CLEAN);
                        // Delete all remote files.
                        saveManager.deleteObjects(FastStream.of(deleteOperations)
                                .map(e -> {
                                    assert e.remote != null;
                                    return e.remote.s3Object;
                                })
                                .toList()
                        );

                        progressTracker.nextStage(SyncStage.SYNC_UP, uploadOperations.size());
                        for (FileOperation op : uploadOperations) {
                            assert op.local != null;

                            totalSize += op.local.size();

                            tasks.add((cancelToken, listener) -> {
                                try {
                                    LOGGER.info("Uploading file to S3: {}", op.local.path());
                                    if (op.remote != null) {
                                        saveManager.uploadFile(op.local.path, op.remote.s3Object.key(), listener);
                                    } else {
                                        String key = instance.getUuid() + "/" + instance.getDir().relativize(op.local.path);
                                        saveManager.uploadFile(op.local.path, key, listener);
                                    }
                                } finally {
                                    progressTracker.stepFinished();
                                }
                            });
                        }
                    } else {
                        assert direction == SyncDirection.DOWNLOAD;
                        assert uploadOperations.isEmpty();
                        progressTracker.nextStage(SyncStage.CLEAN, deleteOperations.size());

                        // Delete all local files.
                        for (FileOperation op : deleteOperations) {
                            assert op.local != null;
                            LOGGER.info("Deleting local file {}", op.local.path);
                            Files.delete(op.local.path);
                            progressTracker.stepFinished();
                        }

                        progressTracker.nextStage(SyncStage.SYNC_DOWN, downloadOperations.size());
                        for (FileOperation op : downloadOperations) {
                            assert op.remote != null;

                            totalSize += op.remote.size();

                            tasks.add((cancelToken, listener) -> {
                                try {
                                    LOGGER.info("Downloading file from S3: {}", op.remote.path);
                                    if (op.local != null) {
                                        saveManager.downloadFile(op.local.path, op.remote.s3Object, listener);
                                    } else {
                                        Path path = instance.getDir().resolve(op.remote.path);
                                        saveManager.downloadFile(path, op.remote.s3Object, listener);
                                    }
                                } finally {
                                    progressTracker.stepFinished();
                                }
                            });
                        }
                    }

                    TaskProgressListener listener = progressTracker.listenerForStage();
                    listener.start(totalSize);
                    TaskProgressAggregator aggregator = new ParallelTaskProgressAggregator(listener);
                    ParallelTaskHelper.executeInParallel(null, Task.TASK_POOL, tasks, aggregator);
                } catch (Throwable ex) {
                    LOGGER.error("Failed to process sync, instance may be in an invalid state.", ex);
                    syncError = ex;
                }
            }
            progressTracker.nextStage(SyncStage.POST_UPDATE);
            boolean jsonUpdated = false;
            try {
                manifest.lastSync = System.currentTimeMillis();
                manifest.state = syncError == null ? SyncManifest.State.SYNCED : direction == SyncDirection.UPLOAD ? SyncManifest.State.UNFINISHED_UP : UNFINISHED_DOWN;
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
                    saveManager.uploadFile(syncManifestFile, instance.getUuid() + "/sync_manifest.json", null);
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

            if (direction == SyncDirection.DOWNLOAD) {
                progressTracker.nextStage(SyncStage.POST_RUN);
                instance.syncFinished();

                LOGGER.info("Validating ModLoader installation..");
                ModpackVersionManifest.Target gameTarget = instance.versionManifest.findTarget("game");
                if (gameTarget != null) {
                    ModpackVersionManifest.Target modloaderTarget = instance.versionManifest.findTarget("modloader");
                    if (modloaderTarget != null) {
                        ModLoaderInstallTask modLoaderInstallTask;
                        try {
                            modLoaderInstallTask = ModLoaderInstallTask.createInstallTask(
                                    instance,
                                    gameTarget.getVersion(),
                                    modloaderTarget.getName(),
                                    modloaderTarget.getVersion()
                            );
                        } catch (Throwable ex) {
                            LOGGER.error("Failed to prepare ModLoader install task.", ex);
                            throw ex;
                        }
                        try {
                            modLoaderInstallTask.execute(null, null);
                        } catch (Throwable ex) {
                            LOGGER.error("Failed to execute ModLoader install task.", ex);
                            SneakyUtils.throwUnchecked(ex);
                        }
                    }
                } else {
                    LOGGER.error("Game target missing from version manifest?");
                }
            }

            LOGGER.info("Cloud sync finished!");
        } finally {
            progressTracker.finished();
        }
    }

    private Map<String, LocalFile> indexInstance() throws IOException {
        ImmutableMap.Builder<String, LocalFile> builder = ImmutableMap.builder();
        Path rootDir = instance.getDir();
        try (Stream<Path> stream = Files.walk(rootDir)) {
            stream.forEach(e -> {
                if (!Files.isDirectory(e)) {
                    LocalFile path = new LocalFile(e);
                    // Skip ignored files.
                    if (shouldSkipFile(path.path().replace('\\', '/'))) return;

                    builder.put(path.path(), path);
                }
            });
        }
        return builder.build();
    }

    private static boolean shouldSkipFile(String relpath) {
        relpath = relpath.toLowerCase(Locale.ROOT);
        for (String ignore : IGNORED_FILES) {
            if (relpath.startsWith(ignore)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, RemoteFile> indexCloud(Map<String, S3Object> s3ObjectIndex) {
        ImmutableMap.Builder<String, RemoteFile> builder = ImmutableMap.builder();
        for (S3Object s3Object : s3ObjectIndex.values()) {
            CompletableFuture<Map<String, String>> metadataFuture = CompletableFuture.supplyAsync(() -> saveManager.getMetadata(s3Object), Task.TASK_POOL);
            RemoteFile path = new RemoteFile(s3Object, metadataFuture);

            // Don't index sync_manifest.json
            if (path.path().equals("sync_manifest.json")) continue;

            builder.put(path.path(), path);
        }
        return builder.build();
    }

    // TODO, Do we really need a sync state for UNFINISHED_DOWNLOAD? Should be safe to just do whatever?
    private static SyncDirection determineDirection(@Nullable SyncManifest remoteManifest, @Nullable InstanceJson remoteInstance, @Nullable SyncManifest localManifest, InstanceJson localInstance) throws ConflictException {
        if (remoteManifest == null) return SyncDirection.UPLOAD; // Sync manifest does not exist on remote, must be UPLOAD.
        if (localManifest == null) return SyncDirection.DOWNLOAD; // We have never tried syncing this instance, and remote manifest exists. Must be DOWNLOAD.

        if (remoteManifest.state == SYNCING) {
            throw new ConflictException("another_sync", "Another FTBApp instance is already syncing this instance."); // TODO We need to check the timestamp?
        }
        if (localManifest.state == UNFINISHED_UP) {
            if (remoteManifest.state != UNFINISHED_UP) {
                // TODO perhaps we just download?
                throw new ConflictException("another_resolved_up", "This FTBApp instance thought it still had files to upload, but the remote does not. Another FTBApp instance has already resolved this error.");
            }
            // Resume the upload
            // TODO check timestamp? if it does not match ours then another FTBA failed to sync up?
            return SyncDirection.UPLOAD;
        }
        if (localManifest.state == UNFINISHED_DOWN) {
            if (remoteManifest.state != UNFINISHED_DOWN) {
                // TODO, perhaps we just download?
                throw new ConflictException("another_resolved_down", "This FTBApp instance thought it still had files to download, but the remote does not. Another FTBApp instance has already resolved this error.");
            }
            // Resume the download.
            // TODO Check timestamp? if it does not match ours then another FTBA failed to download? Do we care?
            return SyncDirection.DOWNLOAD;
        }
        if (remoteManifest.state == UNFINISHED_UP) {
            throw new ConflictException("another_unfinished_up", "Another FTBApp instance has failed to finish uploading changes.");
        }
        if (remoteManifest.state == UNFINISHED_DOWN) {
            throw new ConflictException("another_unfinished_down", "Another FTBApp instance has failed to finish downloading changes.");
        }

        // These can probably be asserts.
        if (localManifest.state != SYNCED) throw new ConflictException("unknown_state_local", "The FTBApp found an unexpected local state.");
        if (remoteManifest.state != SYNCED) throw new ConflictException("unknown_state_remote", "The FTBApp found an unexpected remote state.");

        // In theory this branch should never be hit, only if upload fails partway and the json is not synced.
        if (remoteInstance == null) return SyncDirection.UPLOAD;

        // If remote played last, download.
        if (remoteInstance.lastPlayed > localInstance.lastPlayed) return SyncDirection.DOWNLOAD;
        // If we played last, upload.
        if (remoteInstance.lastPlayed < localInstance.lastPlayed) return SyncDirection.UPLOAD;

        // Up-to-date.
        return SyncDirection.UP_TO_DATE;
    }

    private static int getPathPriority(String path) {
        // Normalize
        path = path.replace('\\', '/').toLowerCase(Locale.ROOT);
        for (int i = 0; i < FILE_PRIORITIES.size(); i++) {
            String prio = FILE_PRIORITIES.get(i);
            if (path.startsWith(prio)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isUnPrioritized(String path) {
        path = path.replace('\\', '/').toLowerCase(Locale.ROOT);
        for (String prio : FILE_UN_PRIORITIES) {
            if (path.startsWith(prio)) {
                return true;
            }
        }
        return false;
    }

    public enum SyncDirection {
        UP_TO_DATE,
        DOWNLOAD,
        UPLOAD
    }

    public record FileOperation(OperationKind kind, @Nullable LocalFile local, @Nullable RemoteFile remote) implements Comparable<FileOperation> {

        @Override
        public int compareTo(FileOperation o) {
            if (local != null) {
                return local.compareTo(o.local);
            }
            if (remote != null) {
                return remote.compareTo(o.remote);
            }

            assert kind == OperationKind.DELETE;
            throw new UnsupportedOperationException("Unable to sort Deletes");
        }
    }

    public enum OperationKind {
        DELETE,
        UPLOAD,
        DOWNLOAD,
    }

    public enum SyncStage implements OperationProgressTracker.Stage {
        PREPARE,
        CONFLICT,
        INDEXING_LOCAL,
        INDEXING_REMOTE,
        COMPUTING_CHANGES,
        BEGIN_SYNC,
        CLEAN,
        SYNC_UP,
        SYNC_DOWN,
        POST_UPDATE,
        POST_RUN,
    }

    public static class ConflictException extends Exception {

        public final String code;
        public final String message;

        public ConflictException(String code, String message) {
            this.code = code;
            this.message = message;
        }
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
    private class LocalFile extends IndexedFile implements Comparable<LocalFile> {

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

        @Override
        public int compareTo(LocalFile o) {
            int pA = getPathPriority(pathStr);
            int pB = getPathPriority(o.pathStr);

            if (pA != -1 && pB == -1) return -1; // We come first.
            if (pB != -1 && pA == -1) return 1; // We come later.
            if (pA != pB) return Integer.compare(pA, pB);

            boolean unA = isUnPrioritized(pathStr);
            boolean unB = isUnPrioritized(o.pathStr);
            if (unA && !unB) return 1;
            if (!unA && unB) return -1;

            return pathStr.compareTo(o.pathStr);
        }
    }

    private class RemoteFile extends IndexedFile implements Comparable<RemoteFile> {

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

        @Override
        public int compareTo(RemoteFile o) {
            int pA = getPathPriority(path);
            int pB = getPathPriority(o.path);

            if (pA != -1 && pB == -1) return -1; // We come first.
            if (pB != -1 && pA == -1) return 1; // We come later.
            if (pA != pB) return Integer.compare(pA, pB);

            boolean unA = isUnPrioritized(path);
            boolean unB = isUnPrioritized(o.path);
            if (unA && !unB) return 1;
            if (!unA && unB) return -1;

            return path.compareTo(o.path);
        }
    }
}
