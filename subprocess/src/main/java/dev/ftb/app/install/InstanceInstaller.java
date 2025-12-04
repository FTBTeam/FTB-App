package dev.ftb.app.install;

import com.google.common.hash.HashCode;
import dev.ftb.app.AppMain;
import dev.ftb.app.data.InstanceModifications;
import dev.ftb.app.data.InstanceModifications.ModOverride;
import dev.ftb.app.data.InstanceModifications.ModOverrideState;
import dev.ftb.app.data.modpack.ModpackVersionManifest;
import dev.ftb.app.data.modpack.ModpackVersionManifest.ModpackFile;
import dev.ftb.app.data.modpack.ModpackVersionModsManifest;
import dev.ftb.app.install.OperationProgressTracker.DefaultStages;
import dev.ftb.app.install.tasks.*;
import dev.ftb.app.install.tasks.modloader.ModLoaderInstallTask;
import dev.ftb.app.instance.InstanceOperation;
import dev.ftb.app.pack.CancellationToken;
import dev.ftb.app.pack.Instance; 
import dev.ftb.app.util.FileUtils;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.io.IOUtils;
import net.covers1624.quack.util.MultiHasher;
import net.covers1624.quack.util.MultiHasher.HashFunc;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static dev.ftb.app.data.modpack.ModpackVersionManifest.GSON;
import static dev.ftb.app.data.modpack.ModpackVersionManifest.Target;
import static net.covers1624.quack.util.SneakyUtils.nullCons;

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

    private final OperationProgressTracker tracker;
    private final CancellationToken cancelToken;

    @Nullable
    private final ModpackVersionManifest oldManifest;

    private final OperationType operationType;

    private @Nullable
    final ModpackVersionModsManifest oldMods;
    private @Nullable
    final ModpackVersionModsManifest newMods;

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
    private final List<ModOverride> overridesToRemove = new LinkedList<>();

    /**
     * Any files that will be downloaded.
     */
    private final List<Path> filesToDownload = new LinkedList<>();

    /**
     * new id -> old id
     */
    private final Map<Long, Long> fileUpdateMap = new HashMap<>();

    private final List<ModOverride> newOverrides = new LinkedList<>();
    //endregion

    private final List<DlTask> tasks = new LinkedList<>();

    @Nullable
    private ModLoaderInstallTask modLoaderInstallTask;

    @Nullable
    private Map<String, IndexedFile> knownFiles;

    public InstanceInstaller(Instance instance, ModpackVersionManifest manifest, CancellationToken cancelToken, OperationProgressTracker tracker) throws IOException {
        super(instance, manifest);
        this.cancelToken = cancelToken;
        this.tracker = tracker;

        ModpackVersionModsManifest oldMods = null;
        ModpackVersionModsManifest newMods = null;

        Path instanceVersionFile = instance.metaPath.resolve(Instance.VERSION_FILE_NAME);
        if (!Files.exists(instanceVersionFile)) {
            oldManifest = null;
            operationType = OperationType.FRESH_INSTALL;
        } else {
            oldManifest = JsonUtils.parse(GSON, instanceVersionFile, ModpackVersionManifest.class);
            if (oldManifest.getId() == manifest.getId()) {
                operationType = OperationType.VALIDATE;
            } else {
                operationType = OperationType.UPGRADE;
                oldMods = instance.getModsManifest();
                try {
                    newMods = ModpackVersionModsManifest.query(instance.props.id, manifest.getId(), instance.props._private, instance.props.packType);
                } catch (IOException ex) {
                    LOGGER.error("Failed to query mods manifest.", ex);
                }
            }
        }

        this.oldMods = oldMods;
        this.newMods = newMods;
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

    public Instance getInstance() {
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
            String gameVersion = manifest.getTargetVersion("game");
            if (gameVersion != null && !instance.props.mcVersion.equals(gameVersion)) {
                LOGGER.warn("Instance had invalid mcVersion attribute. Repairing..");
                instance.props.mcVersion = gameVersion;
            }
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

            InstanceModifications modifications = instance.getModifications();
            if (modifications != null) {
                LOGGER.info("Removing dead overrides..");
                overridesToRemove.forEach(modifications.getOverrides()::remove);
                instance.saveModifications();
            }

            tracker.nextStage(InstallStage.MOD_LOADER);
            if (modLoaderInstallTask != null) {
                LOGGER.info("Installing ModLoader..");
                modLoaderInstallTask.execute(cancelToken, null);
                instance.props.modLoader = modLoaderInstallTask.getModLoaderTarget();
            } else {
                // Mod loader doesn't exist. This must be vanilla
                instance.props.modLoader = manifest.getTargetVersion("game");
            }

            cancelToken.throwIfCancelled();

            LOGGER.info("Downloading new files.");
            tracker.nextStage(InstallStage.FILES, tasks.size());
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
                
                try (ZipFile zipFile = new ZipFile.Builder().setFile(cfOverrides.toFile()).get()) {
                    Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
                    while (entries.hasMoreElements()) {
                        var entry = entries.nextElement();
                        
                        if (entry.isDirectory()) continue; // Skip directories
                        if (!entry.getName().startsWith("overrides/")) continue; // Only process overrides
                        
                        Path path = instance.getDir().resolve(FileUtils.stripInvalidChars(entry.getName()).replace("overrides/", ""));
                        if (Files.exists(path)) {
                            continue;
                        }

                        Files.createDirectories(path.getParent());
                        try (var inputStream = zipFile.getInputStream(entry)) {
                            Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
            }

            cancelToken.throwIfCancelled();

            if (modifications != null) {
                LOGGER.info("Adding new overrides..");
                modifications.getOverrides().addAll(newOverrides);
                instance.saveModifications();
            }

            JsonUtils.write(GSON, instance.metaPath.resolve(Instance.VERSION_FILE_NAME), manifest);

            instance.props.installComplete = true;
            instance.props.versionId = manifest.getId();
            instance.props.version = manifest.getName();
            instance.versionManifest = manifest;
            try {
                instance.saveJson();
            } catch (IOException ex) {
                throw new InstallationFailureException("Failed to save instance json.", ex);
            }
            tracker.nextStage(DefaultStages.FINISHED);
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
            IndexedFile modpackFile = entry.getValue();
            Path file = remapFileFromOverride(
                    modpackFile,
                    instance.getDir().resolve(entry.getKey()),
                    nullCons()
            );
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

        // Figure out which file paths just don't exist anymore.
        Map<String, IndexedFile> newFiles = getKnownFiles();
        Map<String, IndexedFile> oldFiles = computeKnownFiles(oldManifest);
        for (String oldFilePath : oldFiles.keySet()) {
            if (!newFiles.containsKey(oldFilePath)) {
                IndexedFile file = oldFiles.get(oldFilePath);
                Path path = remapFileFromOverride(
                        file,
                        instance.getDir().resolve(oldFilePath),
                        overridesToRemove::add
                );
                filesToRemove.add(path);
            }
        }

        Map<Long, Long> projectLookup = new HashMap<>();
        if (oldMods != null && newMods != null) {
            for (ModpackVersionModsManifest.Mod mod : newMods.getMods()) {
                long curseProj = mod.getCurseProject();
                if (curseProj == -1) continue;
                projectLookup.put(curseProj, mod.getFileId());
            }
            for (ModpackVersionModsManifest.Mod mod : oldMods.getMods()) {
                long curseProj = mod.getCurseProject();
                if (curseProj == -1) continue;
                Long newFileId = projectLookup.get(curseProj);
                if (newFileId == null) continue;
                fileUpdateMap.put(newFileId, mod.getFileId());
            }
        }

        // Ensure the mods lookup cache for the previous version gets nuked on upgrade.
        Path meta = instance.getDir().resolve(".ftba/version_mods.json");
        if (Files.exists(meta)) {
            filesToRemove.add(meta);
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
                for (Path path : files.toList()) {
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
        InstanceModifications modifications = instance.getModifications();

        Target modLoaderTarget;
        if (modifications != null && modifications.getModLoaderOverride() != null) {
            modLoaderTarget = modifications.getModLoaderOverride();
        } else {
            modLoaderTarget = manifest.findTarget("modloader");
        }
        if (modLoaderTarget != null) {
            try {
                modLoaderInstallTask = ModLoaderInstallTask.createInstallTask(
                        instance,
                        instance.getMcVersion(),
                        modLoaderTarget.getName(),
                        modLoaderTarget.getVersion()
                );
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to prepare ModLoader install task.", ex);
            }
        }
    }

    private void prepareFileDownloads() {
        for (ModpackFile file : manifest.getFiles()) {
            if (file.getType().equals("cf-extract")) continue;
            Path filePath = file.toPath(instance.getDir());
            ModOverride override = findModOverride(fileUpdateMap.get(file.getId()));
            if (override != null) {
                filePath = mapFileName(filePath, override.getState());

                ModOverride newOverride = new ModOverride(
                        override.getState(),
                        file.getName(),
                        file.getId()
                );
                newOverrides.add(newOverride);
            }
            
            // Last line of defense against zero byte files.
            if (file.getSize() == 0) {
                filesToDownload.add(filePath);
                tasks.add(new DlTask(0, new EmptyFileDlTask(filePath)));
                continue;
            }

            DownloadTask task = DownloadTask.builder()
                    .url(file.getUrl())
                    .withMirrors(file.getMirror())
                    .dest(filePath)
                    .withValidation(file.createValidation().asDownloadValidation())
                    .withFileLocator(AppMain.localCache)
                    .build();
            if (!task.isRedundant()) {
                long size = file.getSize();
                if (size <= 0) {
                    size = DownloadTask.getContentLength(file.getUrl());
                }
                filesToDownload.add(task.getDest());
                tasks.add(new DlTask(size, task));
            }
        }
    }

    private Path remapFileFromOverride(IndexedFile file, Path path, Consumer<ModOverride> cons) {
        ModOverride override = findModOverride(file.fileName());
        if (override != null) {
            LOGGER.info("File {} has override!", path);
            path = mapFileName(path, override.getState());
            cons.accept(override);
            LOGGER.info("Remapped file path to {} for state {}.", path, override.getState());
        }
        return path;
    }

    private Path mapFileName(Path path, ModOverrideState state) {
        if (state == ModOverrideState.DISABLED) return path.resolveSibling(path.getFileName() + ".disabled");
        if (state == ModOverrideState.ENABLED) return path.resolveSibling(StringUtils.stripEnd(path.getFileName().toString(), ".disabled"));
        return path;
    }

    private @Nullable ModOverride findModOverride(@Nullable Long fileId) {
        if (fileId == null) return null;

        InstanceModifications modifications = instance.getModifications();
        if (modifications == null) return null;

        return modifications.findOverride(fileId);
    }

    private @Nullable ModOverride findModOverride(String fName) {
        InstanceModifications modifications = instance.getModifications();
        if (modifications == null) return null;

        return modifications.findOverride(fName);
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

    public enum InstallStage implements OperationProgressTracker.Stage {
        PREPARE,
        MOD_LOADER,
        FILES,
    }

    public record InvalidFile(
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

    private class DlTask implements Task {

        private final long size;
        private final Task task;

        private DlTask(long size, Task task) {
            this.size = size;
            this.task = task;
        }

        @Override
        public void execute(@Nullable CancellationToken cancelToken, @Nullable TaskProgressListener listener) throws Throwable {
            task.execute(cancelToken, listener);
            tracker.stepFinished();
        }
    }

    private record EmptyFileDlTask(Path destination) implements Task {
        @Override
        public void execute(@Nullable CancellationToken cancelToken, @Nullable TaskProgressListener listener) throws Throwable {
            LOGGER.info("Ignoring zero byte file: {}", this.destination);

            // Just create an empty file.
            try {
                if (Files.exists(this.destination)) {
                    LOGGER.warn("Zero byte file destination already exists, skipping creation: {}", this.destination);
                    return;
                }
                
                Files.createFile(IOUtils.makeParents(this.destination));
            } catch (IOException e) {
                LOGGER.error("Failed to create empty file: {}", this.destination, e);
            }
        }
    }
}
