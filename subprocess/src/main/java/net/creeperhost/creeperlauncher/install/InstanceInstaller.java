package net.creeperhost.creeperlauncher.install;

import com.google.common.hash.HashCode;
import net.covers1624.quack.collection.ColUtils;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.io.IOUtils;
import net.covers1624.quack.util.MultiHasher;
import net.covers1624.quack.util.MultiHasher.HashFunc;
import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest;
import net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest.ModpackFile;
import net.creeperhost.creeperlauncher.install.InstallProgressTracker.DlFile;
import net.creeperhost.creeperlauncher.install.InstallProgressTracker.InstallStage;
import net.creeperhost.creeperlauncher.install.tasks.*;
import net.creeperhost.creeperlauncher.install.tasks.modloader.ModLoaderInstallTask;
import net.creeperhost.creeperlauncher.instance.InstanceOperation;
import net.creeperhost.creeperlauncher.pack.CancellationToken;
import net.creeperhost.creeperlauncher.pack.LocalInstance;
import net.creeperhost.creeperlauncher.util.PathFixingCopyingFileVisitor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest.GSON;
import static net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest.Target;

/**
 * Created by covers1624 on 3/2/22.
 */
public class InstanceInstaller extends InstanceOperation {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final boolean DEBUG = Boolean.getBoolean("InstanceInstaller.debug");

    // These folders are 'known' to be important in the installation process.
    private static final Set<String> KNOWN_IMPORTANT_FOLDERS = Set.of(
            "mods",
            "coremods",
            "instmdos",
            "config",
            "resources",
            "scripts"
    );

    private final InstallProgressTracker tracker;
    private final CancellationToken cancelToken;

    @Nullable
    private final ModpackVersionManifest oldManifest;

    private final OperationType operationType;

    //region Tracking
    /**
     * Any files which don't pass validation.
     * <p>
     * An entry will only exist here if:
     * <pre>
     * - When updating:
     *   - It exists in both new and old manifests.
     *   - The file hashes and sizes and 'versions' are the same in both manifests
     *     and the file on disk does not match. This can be considered
     *     a 'modified' file.
     * - When validating:
     *   - The file does not exist on disk, but does exist in the manifest.
     *     - In this case, the {@code actualHash} will be @{code null} and
     *       {@code actualSize} will be {@code -1}.
     *   - The file exists on disk and does not pass hash and size validation.
     * </pre>
     */
    private final List<InvalidFile> invalidFiles = new LinkedList<>();

    /**
     * Any files which are in a known 'tracked' directory (mods, etc),
     * but are not a part of any manifest.
     */
    private final List<Path> untrackedFiles = new LinkedList<>();

    /**
     * Any files the Update/Validation operations have determined to be removed.
     */
    private final List<Path> filesToRemove = new LinkedList<>();

    /**
     * Any files that will be downloaded.
     */
    private final List<Path> filesToDownload = new LinkedList<>();
    //endregion

    private final List<DlTask> tasks = new LinkedList<>();

    @Nullable
    private ModLoaderInstallTask modLoaderInstallTask;

    @Nullable
    private Map<String, IndexedFile> knownFiles;

    public InstanceInstaller(LocalInstance instance, ModpackVersionManifest manifest, CancellationToken cancelToken, InstallProgressTracker tracker) throws IOException {
        super(instance, manifest);
        this.cancelToken = cancelToken;
        this.tracker = tracker;

        Path instanceVersionFile = instance.getDir().resolve("version.json");
        if (!Files.exists(instanceVersionFile)) {
            oldManifest = null;
            operationType = OperationType.FRESH_INSTALL;
        } else {
            oldManifest = JsonUtils.parse(GSON, instanceVersionFile, ModpackVersionManifest.class);
            if (oldManifest.getId() == manifest.getId()) {
                operationType = OperationType.VALIDATE;
            } else {
                operationType = OperationType.UPGRADE;
            }
        }

    }

    /**
     * Gets the type of operation this Installer will perform
     * on the instance.
     *
     * @return The OperationType.
     */
    public OperationType getOperationType() {
        return operationType;
    }

    public List<InvalidFile> getInvalidFiles() {
        return invalidFiles;
    }

    public List<Path> getUntrackedFiles() {
        return untrackedFiles;
    }

    public List<Path> getFilesToRemove() {
        return filesToRemove;
    }

    public List<Path> getFilesToDownload() {
        return filesToDownload;
    }

    public LocalInstance getInstance() {
        return instance;
    }

    public CancellationToken getCancelToken() {
        return cancelToken;
    }

    /**
     * Prepare the Installer.
     * <p>
     * Figures out what operations will need to be performed for display and tracking.
     * <p>
     * Sets up internal state ready for the installation process.
     */
    public void prepare() {
        tracker.nextStage(InstallStage.PREPARE);
        if (operationType == OperationType.VALIDATE) {
            validateFiles();
        } else if (operationType == OperationType.UPGRADE) {
            processUpgrade();
        }
        locateUntrackedFiles();
        prepareModLoader();
        prepareFileDownloads();

        if (DEBUG) {
            LOGGER.info("Found the following invalid files:");
            for (InvalidFile invalidFile : getInvalidFiles()) {
                LOGGER.info("  " + invalidFile);
            }

            LOGGER.info("Found the following untracked files:");
            for (Path untrackedFile : getUntrackedFiles()) {
                LOGGER.info("  " + untrackedFile);
            }

            LOGGER.info("The following files will be deleted:");
            for (Path path : getFilesToRemove()) {
                LOGGER.info("  " + path);
            }

            LOGGER.info("The following files will be downloaded:");
            for (Path path : getFilesToDownload()) {
                LOGGER.info("  " + path);
            }
        }
    }

    public void execute() throws InstallationFailureException {
        try {
            LOGGER.info("Removing files..");
            for (Path path : filesToRemove) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ex) {
                    throw new InstallationFailureException("Failed to delete file: '" + path + "'.", ex);
                }
            }

            tracker.nextStage(InstallStage.MODLOADER);
            if (modLoaderInstallTask != null) {
                LOGGER.info("Installing ModLoader..");
                modLoaderInstallTask.execute(cancelToken, null);
                instance.modLoader = modLoaderInstallTask.getResult();
            } else {
                // Mod loader doesn't exist. This must be vanilla
                instance.modLoader = manifest.getTargetVersion("game");
            }

            cancelToken.throwIfCancelled();

            LOGGER.info("Downloading new files.");
            tracker.nextStage(InstallStage.DOWNLOADS);
            long totalSize = tasks.stream()
                    .mapToLong(e -> e.size)
                    .sum();
            TaskProgressListener rootListener = tracker.listenerForStage();
            rootListener.start(totalSize);
            TaskProgressAggregator progressAggregator = new ParallelTaskProgressAggregator(rootListener);

            ParallelTaskHelper.executeInParallel(cancelToken, Task.TASK_POOL, tasks, progressAggregator);

            rootListener.finish(progressAggregator.getProcessed());

            cancelToken.throwIfCancelled();

            Path cfOverrides = getCFOverridesZip(manifest);
            if (cfOverrides != null) {
                LOGGER.info("Extracting CurseForge overrides.");
                try (FileSystem fs = IOUtils.getJarFileSystem(cfOverrides, true)) {
                    Path root = fs.getPath("/overrides/");
                    Files.walkFileTree(root, new PathFixingCopyingFileVisitor(root, instance.getDir()));
                }
            }

            cancelToken.throwIfCancelled();

            JsonUtils.write(GSON, instance.getDir().resolve("version.json"), manifest);

            instance.installComplete = true;
            instance.versionId = manifest.getId();
            instance.version = manifest.getName();
            instance.versionManifest = manifest;
            try {
                instance.saveJson();
            } catch (IOException ex) {
                throw new InstallationFailureException("Failed to save instance json.", ex);
            }
            tracker.nextStage(InstallStage.FINISHED);
            LOGGER.info("Install finished!");
        } catch (InstallationFailureException | CancellationToken.Cancellation ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new InstallationFailureException("Failed to install.", ex);
        }
    }

    public void cancel(CompletableFuture<?> future) {
        cancelToken.cancel(future);
    }

    private void validateFiles() {
        Map<String, IndexedFile> knownFiles = getKnownFiles();
        for (Map.Entry<String, IndexedFile> entry : knownFiles.entrySet()) {
            Path file = instance.getDir().resolve(entry.getKey());
            IndexedFile modpackFile = entry.getValue();
            if (Files.notExists(file)) {
                invalidFiles.add(new InvalidFile(file, modpackFile.sha1(), null, modpackFile.length(), -1));
            } else {
                try {
                    FileValidation validation = modpackFile.createValidation();
                    MultiHasher hasher = new MultiHasher(HashFunc.SHA1);
                    hasher.load(file);
                    MultiHasher.HashResult result = hasher.finish();
                    if (!validation.validate(file, result)) {
                        invalidFiles.add(new InvalidFile(
                                file,
                                modpackFile.sha1(),
                                result.get(HashFunc.SHA1),
                                modpackFile.length(),
                                Files.size(file)
                        ));
                    }
                } catch (IOException ex) {
                    throw new IllegalStateException("Failed to validate file '" + file + "'.", ex);
                }
            }
        }
    }

    private void processUpgrade() {
        assert oldManifest != null;

        Map<String, IndexedFile> newFiles = getKnownFiles();
        Map<String, IndexedFile> oldFiles = computeKnownFiles(oldManifest);
        for (String oldFilePath : oldFiles.keySet()) {
            if (!newFiles.containsKey(oldFilePath)) {
                filesToRemove.add(instance.getDir().resolve(oldFilePath));
            }
        }
    }

    private void locateUntrackedFiles() {
        // TODO, open this up to more folders? Should we ignore configs?
        Set<String> untrackedFolders = Set.of("mods");
        for (String toSearch : untrackedFolders) {
            Path folder = instance.getDir().resolve(toSearch);
            if (Files.notExists(folder)) return;

            Map<String, IndexedFile> knownFiles = getKnownFiles();
            try (Stream<Path> files = Files.walk(folder)) {
                for (Path path : ColUtils.iterable(files)) {
                    if (Files.isDirectory(path)) continue; // Skip directories
                    if (filesToRemove.contains(path)) continue; // File will be deleted, ignore.
                    if (knownFiles.containsKey(instance.getDir().relativize(path).toString())) continue; // File is known.

                    if (path.getFileName().toString().startsWith("__tmp_")) {
                        // Download temp files. Always delete these.
                        // They will only exist if the download process crashes spectacularly.
                        filesToRemove.add(path);
                    } else {
                        untrackedFiles.add(path);
                    }
                }
            } catch (IOException ex) {
                throw new IllegalStateException("An IO Error occurred whilst locating untracked files.", ex);
            }
        }
    }

    private void prepareModLoader() {
        Target gameTarget = manifest.findTarget("game");
        assert gameTarget != null;
        assert gameTarget.getName().equals("minecraft");

        Target modLoaderTarget = manifest.findTarget("modloader");
        if (modLoaderTarget != null) {
            try {
                modLoaderInstallTask = ModLoaderInstallTask.createInstallTask(
                        instance,
                        gameTarget.getVersion(),
                        modLoaderTarget.getName(),
                        modLoaderTarget.getVersion()
                );
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to prepare ModLoader install task.", ex);
            }
        }
    }

    private void prepareFileDownloads() {
        List<DlFile> dlFiles = new LinkedList<>();
        for (ModpackFile file : manifest.getFiles()) {
            if (file.getType().equals("cf-extract")) continue;
            NewDownloadTask task = NewDownloadTask.builder()
                    .url(file.getUrl())
                    .dest(file.toPath(instance.getDir()))
                    .withValidation(file.createValidation().asDownloadValidation())
                    .withFileLocator(CreeperLauncher.localCache)
                    .build();
            if (!task.isRedundant()) {
                long size = file.getSize();
                if (size <= 0) {
                    size = NewDownloadTask.getContentLength(file.getUrl());
                }
                filesToDownload.add(task.getDest());
                tasks.add(new DlTask(file.getId(), size, task));
                dlFiles.add(new DlFile(file.getId(), file.getName()));
            }
        }
        tracker.submitFiles(dlFiles);
    }

    /**
     * Defines what type of operation will be performed on the instance.
     */
    public enum OperationType {
        /**
         * This is a completely fresh installation, nothing has been installed before.
         */
        FRESH_INSTALL,
        /**
         * This is an upgrade, a modpack is already installed in this instance.
         */
        UPGRADE,
        /**
         * This is a validation pass, this is effectively an UPGRADE but using the existing instance manifest.
         */
        VALIDATE,
    }

    public static record InvalidFile(
            Path path,
            @Nullable HashCode expectedHash,
            @Nullable HashCode actualHash,
            long expectedLen,
            long actualLen
    ) { }

    public static class InstallationFailureException extends Exception {

        public InstallationFailureException(String message) {
            super(message);
        }

        public InstallationFailureException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private class DlTask implements Task<Object> {

        private final long id;
        private final long size;
        private final Task<?> task;

        private DlTask(long id, long size, Task<?> task) {
            this.id = id;
            this.size = size;
            this.task = task;
        }

        @Override
        public void execute(@Nullable CancellationToken cancelToken, @Nullable TaskProgressListener listener) throws Throwable {
            task.execute(cancelToken, listener);
            tracker.fileFinished(id);
        }

        @Override
        @Nullable
        public Object getResult() {
            return task.getResult();
        }
    }
}
