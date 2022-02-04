package net.creeperhost.creeperlauncher.install;

import com.google.common.hash.HashCode;
import net.covers1624.quack.collection.StreamableIterable;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.net.DownloadAction;
import net.covers1624.quack.net.okhttp.OkHttpDownloadAction;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest;
import net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest.ModpackFile;
import net.creeperhost.creeperlauncher.install.tasks.NewDownloadTask;
import net.creeperhost.creeperlauncher.install.tasks.Task;
import net.creeperhost.creeperlauncher.install.tasks.modloader.ModLoaderInstallTask;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest.GSON;
import static net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest.Target;

/**
 * Created by covers1624 on 3/2/22.
 */
public class InstanceInstaller {

    // These folders are 'known' to be important in the installation process.
    private static final Set<String> KNOWN_IMPORTANT_FOLDERS = Set.of(
            "mods",
            "coremods",
            "instmdos",
            "config",
            "resources",
            "scripts"
    );

    private final Path instanceDir;
    private final ModpackVersionManifest manifest;

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
     * Any files which are in a known 'tracked' directory (configs, mods, etc),
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
    private final List<Path> toDownload = new LinkedList<>();
    //endregion

    private final List<Task<?>> tasks = new LinkedList<>();

    @Nullable
    private ModLoaderInstallTask modLoaderInstallTask;

    public static void main(String[] args) throws Throwable {
        System.setProperty("ftba.dataDirOverride", "./");
        StringWriter sw = new StringWriter();
        DownloadAction action = new OkHttpDownloadAction()
                .setUserAgent(Constants.USER_AGENT)
                .setUrl(Constants.getCreeperhostModpackPrefix(false, (byte) 0) + "91" + "/" + "2105")
                .setDest(sw);
        action.execute();

        ModpackVersionManifest versionManifest = GSON.fromJson(sw.toString(), ModpackVersionManifest.class);

        InstanceInstaller installer = new InstanceInstaller(Path.of("TestInstall"), versionManifest);
        installer.prepare();

        installer.execute();
    }

    public InstanceInstaller(Path instanceDir, ModpackVersionManifest manifest) throws IOException {
        this.instanceDir = instanceDir;
        this.manifest = manifest;

        Path instanceVersionFile = instanceDir.resolve("version.json");
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

    /**
     * Prepare the Installer.
     * <p>
     * Figures out what operations will need to be performed for display and tracking.
     * <p>
     * Sets up internal state ready for the installation process.
     */
    public void prepare() {
        if (operationType == OperationType.UPGRADE || operationType == OperationType.VALIDATE) {
            // Locate invalid/missing files.
            assert false : "TODO";
        }
        prepareModLoader();
        prepareFileDownloads();
    }

    private void prepareModLoader() {
        Target gameTarget = findTarget("game");
        assert gameTarget != null;
        assert gameTarget.getName().equals("minecraft");

        Target modLoaderTarget = findTarget("modloader");
        if (modLoaderTarget != null) {
            try {
                modLoaderInstallTask = ModLoaderInstallTask.createInstallTask(
                        instanceDir,
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
        for (ModpackFile file : manifest.getFiles()) {
            NewDownloadTask task = NewDownloadTask.builder()
                    .url(file.getUrl())
                    .dest(file.toPath(instanceDir))
                    .withValidation(file.getValidation().asDownloadValidation())
                    .build();
            if (!task.isRedundant()) {
                toDownload.add(task.getDest());
                tasks.add(task);
            }
        }
    }

    public void execute() throws InstallationFailureException {
        try {
            for (Path path : filesToRemove) {
                try {
                    Files.delete(path);
                } catch (IOException ex) {
                    throw new InstallationFailureException("Failed to delete file: '" + path + "'.", ex);
                }
            }

            if (modLoaderInstallTask != null) {
                modLoaderInstallTask.execute(null, null);
            }

            for (Task<?> task : tasks) {
                task.execute(null, null);
            }
        } catch (InstallationFailureException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new InstallationFailureException("Failed to install.", ex);
        }
    }

    /**
     * Finds a {@link Target} of a given type in the installation manifest.
     *
     * @param type The 'type' to find.
     * @return The {@link Target}.
     * @throws IllegalStateException If more than one target of the given type is found.
     */
    @Nullable
    private Target findTarget(String type) throws IllegalStateException {
        List<Target> targetsMatching = StreamableIterable.of(manifest.getTargets())
                .filter(e -> type.equals(e.getType()))
                .toList();

        if (targetsMatching.size() > 1) {
            // Should be impossible??
            String desc = targetsMatching.stream().map(e -> e.getName() + "@" + e.getVersion()).collect(Collectors.joining(", ", "[", "]"));
            throw new IllegalStateException("Found more than one target for type '" + type + "'. " + desc);
        }

        return !targetsMatching.isEmpty() ? targetsMatching.get(0) : null;
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
            HashCode expectedHash,
            @Nullable
            HashCode actualHash,
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
}
