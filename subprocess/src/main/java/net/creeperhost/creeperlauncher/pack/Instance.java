package net.creeperhost.creeperlauncher.pack;

import com.google.common.hash.Hashing;
import com.google.gson.JsonParseException;
import net.covers1624.quack.collection.ColUtils;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.platform.OperatingSystem;
import net.covers1624.quack.util.HashUtils;
import net.creeperhost.creeperlauncher.*;
import net.creeperhost.creeperlauncher.data.InstanceJson;
import net.creeperhost.creeperlauncher.data.InstanceModifications;
import net.creeperhost.creeperlauncher.data.InstanceModifications.ModOverride;
import net.creeperhost.creeperlauncher.data.InstanceModifications.ModOverrideState;
import net.creeperhost.creeperlauncher.data.InstanceSupportMeta;
import net.creeperhost.creeperlauncher.data.mod.CurseMetadata;
import net.creeperhost.creeperlauncher.data.mod.ModInfo;
import net.creeperhost.creeperlauncher.data.modpack.ModpackManifest;
import net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest;
import net.creeperhost.creeperlauncher.data.modpack.ModpackVersionModsManifest;
import net.creeperhost.creeperlauncher.install.tasks.NewDownloadTask;
import net.creeperhost.creeperlauncher.instance.cloud.CloudSaveManager;
import net.creeperhost.creeperlauncher.minecraft.modloader.forge.ForgeJarModLoader;
import net.creeperhost.creeperlauncher.util.CurseMetadataCache.FileMetadata;
import net.creeperhost.creeperlauncher.util.DialogUtil;
import net.creeperhost.creeperlauncher.util.FileUtils;
import net.creeperhost.creeperlauncher.util.ImageUtils;
import net.creeperhost.creeperlauncher.util.MiscUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import javax.annotation.WillNotClose;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class Instance {

    private static final Logger LOGGER = LogManager.getLogger();

    public final Path path;
    public InstanceJson props;
    private @Nullable InstanceModifications modifications;

    private final InstanceLauncher launcher = new InstanceLauncher(this);
    @Nullable
    public CompletableFuture<?> prepareFuture;
    @Nullable
    public CancellationToken prepareToken;
    private int loadingModPort;
    public ModpackVersionManifest versionManifest;

    private boolean pendingCloudInstance;

    private long startTime;

    // Brand-new instance.
    public Instance(@Nullable String name, @Nullable String artPath, @Nullable String category, ModpackManifest modpack, ModpackVersionManifest versionManifest, String mcVersion, boolean isPrivate, byte packType) {
        props = new InstanceJson(modpack, versionManifest, mcVersion, isPrivate, packType);
        if (name != null) {
            props.name = name;
        }

        props.category = Objects.requireNonNullElse(category, "Default");

        path = Settings.getInstancesDir().resolve(folderNameFor(props));
        FileUtils.createDirectories(path);

        this.versionManifest = versionManifest;
        this.processArt(modpack, artPath);

        try {
            saveJson();
        } catch (IOException ex) {
            LOGGER.error("Failed to save instance.", ex);
        }
    }

    // Loading an existing instance.
    public Instance(Path path, Path json) throws IOException {
        this.path = path;
        props = InstanceJson.load(json);
        loadVersionManifest();
    }

    // Pending cloud save instance.
    public Instance(Path path, InstanceJson props, ModpackVersionManifest versionManifest) {
        this.path = path;
        this.props = props;
        this.versionManifest = versionManifest;
        pendingCloudInstance = true;
    }

    private void processArt(ModpackManifest modpack, @Nullable String artPath) {
        if (artPath != null) {
            var pathForArt = Path.of(artPath);
            // TODO: Support webp?
            if (Files.exists(pathForArt) && (artPath.endsWith(".png") || artPath.endsWith(".jpg") || artPath.endsWith(".jpeg"))) {
                try (InputStream is = Files.newInputStream(pathForArt)) {
                    doImportArt(is);
                    return;
                } catch (IOException ex) {
                    LOGGER.error("Failed to import art.", ex);
                }
            }
        }

        ModpackManifest.Art art = modpack.getFirstArt("square");
        if (art != null) {
            Path tempFile = null;
            try {
                tempFile = Files.createTempFile("art", "");
                Files.delete(tempFile);
                NewDownloadTask task = NewDownloadTask.builder()
                        .url(art.getUrl())
                        .dest(tempFile)
                        .build();
                task.execute(null, null);
                try (InputStream is = Files.newInputStream(tempFile)) {
                    doImportArt(is);
                }
            } catch (IOException ex) {
                LOGGER.error("Failed to download art.", ex);
            } finally {
                if (tempFile != null) {
                    try {
                        Files.deleteIfExists(tempFile);
                    } catch (IOException ex) {
                        LOGGER.error("Failed to cleanup temp file from art.", ex);
                    }
                }
            }
        }
    }

    public void syncFinished() throws IOException {
        pendingCloudInstance = false;
        props = InstanceJson.load(getDir().resolve("instance.json"));
        props.cloudSaves = true;
        loadVersionManifest();
    }

    private void loadVersionManifest() throws IOException {
        // Do nothing for pending cloud saves.
        if (pendingCloudInstance) return;

        if (props.installComplete) {
            Path versionJson = path.resolve("version.json");
            if (Files.exists(versionJson)) {
                versionManifest = JsonUtils.parse(ModpackVersionManifest.GSON, versionJson, ModpackVersionManifest.class);
            } else {
                versionManifest = ModpackVersionManifest.makeInvalid();
            }
            Path modificationsJson = path.resolve("modifications.json");
            if (Files.exists(modificationsJson)) {
                // TODO we need to validate the state of mod modifications.
                //      Dynamically add/remove them as manual modifications are always possible.
                //      We should do this any time the Mods list is queried. I.e the frontend or installer requests it.
                modifications = InstanceModifications.load(modificationsJson);
            }
        }
    }

    public void importArt(Path file) throws IOException {
        if (pendingCloudInstance) {
            throw new UnsupportedOperationException("Can't import art for pending cloud instances.");
        }

        try (InputStream is = Files.newInputStream(file)) {
            doImportArt(is);
            saveJson();
        }
    }

    private void doImportArt(@WillNotClose InputStream is) throws IOException {
        BufferedImage resizedArt = ImageUtils.resizeImage(is, 256, 256);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(resizedArt, "png", bos);
        props.art = "data:image/png;base64," + Base64.getEncoder().encodeToString(bos.toByteArray());
        // folder.jpg is not strictly used, it exists for easy folder navigation.
        try (OutputStream os = Files.newOutputStream(path.resolve("folder.jpg"))) {
            ImageIO.write(resizedArt, "jpg", os);
        }
    }

    public synchronized void pollVersionManifest() {
        if (pendingCloudInstance) return; // Do nothing for pending cloud save instances.
        if (props.isImport) return; // Can't update manifests for imports.
        try {
            Pair<ModpackManifest, ModpackVersionManifest> newManifest = ModpackVersionManifest.queryManifests(props.id, props.versionId, props._private, props.packType);
            if (newManifest == null) {
                LOGGER.warn("Failed to update modpack version manifest for instance. This may be a private pack.");
                return;
            }
            versionManifest = newManifest.getRight();
            JsonUtils.write(ModpackVersionManifest.GSON, path.resolve("version.json"), versionManifest, ModpackVersionManifest.class);
        } catch (IOException ex) {
            LOGGER.warn("Failed to update manifest for modpack. This may be a private pack.", ex);
        }
    }

    // TODO, In theory this meta should be getting added to the regular version manifest.
    //       When that happens we can nuke this.
    public @Nullable ModpackVersionModsManifest getModsManifest() {
        ModpackVersionModsManifest manifest = null;
        Path file = path.resolve(".ftba/version_mods.json");
        try {
            manifest = ModpackVersionModsManifest.load(file);
        } catch (IOException | JsonParseException ex) {
            LOGGER.warn("Failed to parse version mods manifest.", ex);
        }

        if (manifest != null) return manifest;

        try {
            manifest = ModpackVersionModsManifest.query(props.id, props.versionId, props._private, props.packType);
            ModpackVersionModsManifest.save(file, manifest);
            return manifest;
        } catch (IOException | JsonParseException ex) {
            LOGGER.warn("Failed to query version mods manifest.", ex);
            return null;
        }
    }

    /**
     * Force stops the instance.
     * <p>
     * Does not block until the instance has stopped.
     */
    public void forceStop() {
        launcher.forceStop();
    }

    /**
     * Starts the instance.
     *
     * @param token     The CancellationToken for cancelling the launch.
     * @param extraArgs Extra JVM arguments.
     * @throws InstanceLaunchException If there was an error preparing or starting the instance.
     */
    public void play(CancellationToken token, String extraArgs, @Nullable String offlineUsername) throws InstanceLaunchException {
        if (pendingCloudInstance) {
            // Technically a UI bug, should display Install/Sync instead of Launch.
            throw new InstanceLaunchException("Cloud instance needs to be installed before it can be launched.");
        }
        if (launcher.isRunning()) {
            throw new InstanceLaunchException("Instance already running.");
        }
        LOGGER.info("Resetting launcher..");
        launcher.reset();
        // TODO, why do we need to do this? Can anything in here change that affects launching? Only the Java versions perhaps?
        LOGGER.info("Polling version manifest.");
        pollVersionManifest();

        LOGGER.info("Scanning instance.");
        InstanceScanner scanner = new InstanceScanner(path, versionManifest);
        scanner.scan();
        if (scanner.isPotentiallyInvalid()) {
            boolean abort = DialogUtil.confirmDialog("Potentially invalid instance", "Abort", "Launch", "Your instance appears to have duplicate mods or invalid scripts.\nIt is highly recommended that you re-install your instance.\nDo you want to abort launching?");
            if (abort) {
                throw new InstanceLaunchException.Abort("Aborted.");
            }
        }

        LOGGER.info("Adding start/shutdown tasks..");
        launcher.withStartTask(ctx -> {
            // TODO, `extraArgs` and `jvmArgs` should be an array
            ctx.extraJVMArgs.addAll(MiscUtils.splitCommand(extraArgs));

            // TODO, do this on LocalInstance load, potentially combine with changes to make jvmArgs an array.
            List<String> jvmArgs = MiscUtils.splitCommand(props.jvmArgs);
            for (Iterator<String> iterator = jvmArgs.iterator(); iterator.hasNext(); ) {
                String jvmArg = iterator.next();
                if (jvmArg.contains("-Xmx")) {
                    iterator.remove();
                    break;
                }
            }
            ctx.extraJVMArgs.addAll(jvmArgs);
        });

        if (CreeperLauncher.CLOUD_SAVE_MANAGER.isConfigured() && props.cloudSaves) {
            launcher.withStartTask(ctx -> {
                LOGGER.info("Attempting start cloud sync..");
                try {
                    CloudSaveManager.SyncResult result = CreeperLauncher.CLOUD_SAVE_MANAGER.requestInstanceSync(this)
                            .get();

                    // Don't error if initial sync is still running, just do nothing.
                    if (result.type() == CloudSaveManager.SyncResult.ResultType.INITIAL_STILL_RUNNING) {
                        return;
                    }
                    if (result.type() != CloudSaveManager.SyncResult.ResultType.SUCCESS) {
                        throw new InstanceLaunchException("Pre-start cloud sync failed! " + result.type() + " " + result.reason());
                    }
                } catch (InterruptedException | ExecutionException ex) {
                    throw new InstanceLaunchException("Failed to wait for start cloud sync.", ex);
                }
            });
            launcher.withExitTask(() -> {
                LOGGER.info("Attempting close cloud sync..");
                // Don't wait on future here, just let it happen in the background.
                CreeperLauncher.CLOUD_SAVE_MANAGER.requestInstanceSync(this);
            });
        }

        if (props.hasInstMods) {
            // TODO, Jar Mods can be done differently
            launcher.withStartTask(ctx -> {
                ForgeJarModLoader.prePlay(this);
            });
        }

        launcher.withStartTask(ctx -> {
            Analytics.sendPlayRequest(getId(), getVersionId(), props.packType);
        });

        OperatingSystem os = OperatingSystem.current();
        launcher.withStartTask(ctx -> {
            // Nuke old files.
            Files.deleteIfExists(path.resolve("Log4jPatcher-1.0.1.jar"));
            Files.deleteIfExists(path.resolve("mods/launchertray-1.0.jar"));
            Files.deleteIfExists(path.resolve("mods/launchertray-progress-1.0.jar"));

            InstanceSupportMeta supportMeta = InstanceSupportMeta.update();
            if (supportMeta == null) return; // Should be _incredibly_ rare. But just incase...

            List<InstanceSupportMeta.SupportFile> loadingMods = supportMeta.getSupportMods("loading");
            if (!loadingMods.isEmpty()) {
                if (Files.notExists(path.resolve(".no_loading_mods.marker"))) {
                    for (InstanceSupportMeta.SupportFile file : loadingMods) {
                        if (!file.canApply(props.modLoader, os)) continue;
                        file.createTask(path.resolve("mods")).execute(null, null);
                    }
                }

                CreeperLauncher.closeOldClient();
                loadingModPort = MiscUtils.getRandomEphemeralPort();
                if (loadingModPort != -1) {
                    CompletableFuture.runAsync(() -> {
                        try {
                            CreeperLauncher.listenForClient(loadingModPort);
                        } catch (BindException err) {
                            LOGGER.error("Error whilst starting mod socket on port '{}'...", loadingModPort, err);
                        } catch (Exception err) {
                            if (!CreeperLauncher.opened) {
                                LOGGER.warn("Error whilst handling message from mod socket - probably nothing!", err);
                                CreeperLauncher.opened = false;
                            }
                        }
                    });
                    ctx.extraJVMArgs.add("-Dchtray.port=" + loadingModPort);
                    ctx.extraJVMArgs.add("-Dchtray.instance=" + props.uuid.toString());
                } else {
                    LOGGER.warn("Failed to find free Ephemeral port.");
                }
            }
            for (InstanceSupportMeta.SupportEntry agent : supportMeta.getSupportAgents()) {
                for (InstanceSupportMeta.SupportFile file : agent.getFiles()) {
                    if (!file.canApply(props.modLoader, os)) continue;
                    file.createTask(path).execute(null, null);
                    ctx.extraJVMArgs.add("-javaagent:" + file.getName());
                }
            }
            if (!scanner.hasLegacyJavaFixer()) {
                for (InstanceSupportMeta.SupportFile file : supportMeta.getSupportMods("legacyjavafixer")) {
                    if (!file.canApply(props.modLoader, os)) continue;
                    file.createTask(path.resolve("mods")).execute(null, null);
                }
            }
        });

        launcher.withStartTask(ctx -> {
            startTime = System.currentTimeMillis();
            props.lastPlayed = startTime / 1000L;
            saveJson();
        });
        launcher.withExitTask(() -> {
            long endTime = System.currentTimeMillis();
            props.totalPlayTime += endTime - startTime;
            saveJson();
        });
        LOGGER.info("Handing off to launcher..");
        launcher.launch(token, offlineUsername);
    }

    public InstanceLauncher getLauncher() {
        return launcher;
    }

    public boolean uninstall() throws IOException {
        if (pendingCloudInstance) {
            // TODO should we wire this up to delete even when not synced?
            throw new NotImplementedException("Unable to delete non-synced cloud instance.");
        }
        FileUtils.deleteDirectory(path);
        Instances.refreshInstances();
        return true;
    }

    public boolean browse() throws IOException {
        return browse("");
    }

    public boolean browse(String extraPath) throws IOException {
        if (pendingCloudInstance) return false;

        if (Files.notExists(path.resolve(extraPath))) {
            return false;
        }

        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(path.resolve(extraPath).toFile());
            return true;
        }
        return false;
    }

    public void setModified(boolean state) {
        if (pendingCloudInstance) throw new UnsupportedOperationException("Can't set un synced cloud instance as modified.");
        props.isModified = state;
    }

    public void saveJson() throws IOException {
        if (pendingCloudInstance) return; // Do nothing for pending cloud save instances.

        // When saving the file we:
        // - write to .json__tmp
        // - move .json -> .json.bak
        // - move .json__tmp -> .json
        Path realJson = path.resolve("instance.json");
        Path backupJson = path.resolve("instance.json.bak");
        Path newJson = path.resolve("instance.json__tmp");
        try {
            InstanceJson.save(newJson, props);
        } catch (AccessDeniedException ex) {
            LOGGER.warn("Failed to write file, access denied. ACL: {}", FileUtils.getAcl(path));
            throw ex;
        }
        if (Files.exists(realJson)) {
            Files.move(realJson, backupJson, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(newJson, realJson);
    }

    public @Nullable InstanceModifications getModifications() {
        return modifications;
    }

    public InstanceModifications getOrCreateModifications() {
        if (modifications == null) {
            modifications = new InstanceModifications();
        }
        return modifications;
    }

    public void saveModifications() {
        try {
            if (modifications != null) {
                Path modificationsJson = path.resolve("modifications.json");
                InstanceModifications.save(modificationsJson, modifications);
            }
        } catch (IOException ex) {
            LOGGER.error("Failed to save instance modifications.", ex);
        }
    }

    @Nullable
    public Instance duplicate(String instanceName) throws IOException {
        if (pendingCloudInstance) throw new UnsupportedOperationException("Can't duplicate un synced cloud instances.");

        InstanceJson json = new InstanceJson(props, UUID.randomUUID(), !instanceName.isEmpty() ? instanceName : props.name);
        json.totalPlayTime = 0;
        json.lastPlayed = 0;

        Path newDir = Settings.getInstancesDir().resolve(folderNameFor(json));
        Path newJson = newDir.resolve("instance.json");

        Files.createDirectories(newDir);

        org.apache.commons.io.FileUtils.copyDirectory(path.toFile(), newDir.toFile());
        InstanceJson.save(newJson, json);

        return new Instance(newDir, newJson);
    }

    public synchronized List<ModInfo> getMods() {
        LOGGER.info("Building instance mods list..");
        List<ModInfo> mods = new ArrayList<>();

        InstanceModifications modifications = getModifications();

        Path modsDir = path.resolve("mods");

        // Populate all mods from the regular version manifest.
        for (ModpackVersionManifest.ModpackFile file : versionManifest.getFiles()) {
            if (!file.getPath().startsWith("./mods") || !isMod(file.getName())) continue;

            String sha1 = Objects.toString(file.getSha1OrNull(), null);

            ModOverride override = modifications != null ? modifications.findOverride(file.getId()) : null;

            boolean fileExists = Files.exists(file.toPath(path));

            // File is in its default state.
            if (override != null && fileExists) {
                assert override.getState() == ModOverrideState.ENABLED || override.getState() == ModOverrideState.DISABLED;
                LOGGER.info("Cleaning up redundant override: {}", override);
                modifications.getOverrides().remove(override);
                override = null;
            }

            if (override != null) {
                // Skip entry, mod has been updated, let the override add the entry.
                if (override.getState() == ModOverrideState.UPDATED_ENABLED || override.getState() == ModOverrideState.UPDATED_DISABLED) {
                    continue;
                }
            }

            // Enabled if override says it is, OR the file exists AND does not have .disabled
            boolean enabled = (override != null && override.getState().enabled()) || (fileExists && !file.getName().endsWith(".disabled"));

            mods.add(new ModInfo(
                    file.getId(),
                    file.getName(),
                    file.getVersionOrNull(),
                    enabled,
                    file.getSize(),
                    sha1,
                    null
            ));
        }

        // Mods added manually or via CurseForge integ.
        if (modifications != null) {
            for (ModOverride override : modifications.getOverrides()) {
                // -1 is for non-pack overrides.
                if (!override.getState().added() && !override.getState().updated()) continue;

                Path file = modsDir.resolve(override.getFileName());
                mods.add(new ModInfo(
                        -1,
                        override.getFileName(),
                        null,
                        override.getState().enabled(),
                        tryGetSize(file),
                        override.getSha1(),
                        CurseMetadata.basic(override.getCurseProject(), override.getCurseFile())
                ));
            }
        }

        for (Path path : FileUtils.listDir(modsDir)) {
            if (!Files.isRegularFile(path)) continue;

            String fName = path.getFileName().toString();
            if (!isMod(fName)) continue;

            String fName2 = StringUtils.stripEnd(fName, ".disabled");
            // Do we already know about the mod?
            if (ColUtils.anyMatch(mods, e -> e.fileName().equals(fName) || e.fileName().equals(fName2))) {
                continue;
            }
            LOGGER.info("Found unknown mod in Mods folder. {}", fName);
            // We don't know about the mod! We need to add it and create a Modification for it.

            String sha1;
            long size;
            try {
                size = Files.size(path);
                sha1 = HashUtils.hash(Hashing.sha1(), path).toString();
            } catch (IOException ex) {
                LOGGER.error("Error reading file. Unable to process this whilst generating mods list.", ex);
                continue;
            }

            long curseProject = -1;
            long curseFile = -1;
            FileMetadata metadata = Constants.CURSE_METADATA_CACHE.queryMetadata(sha1);
            if (metadata != null) {
                LOGGER.info(" Identified as {} {} {}", metadata.name(), metadata.curseProject(), metadata.curseFile());
                curseProject = metadata.curseProject();
                curseFile = metadata.curseFile();
            } else {
                LOGGER.info(" Could not identify mod with hash lookup.");
            }

            ModOverrideState state = fName.endsWith(".disabled") ? ModOverrideState.ADDED_DISABLED : ModOverrideState.ADDED_ENABLED;

            mods.add(new ModInfo(
                    -1,
                    fName2,
                    null,
                    state.enabled(),
                    size,
                    sha1,
                    metadata != null ? metadata.toCurseInfo() : null
            ));

            getOrCreateModifications().getOverrides().add(new ModOverride(state, fName2, sha1, curseProject, curseFile));
            saveModifications();
        }

        LOGGER.info("List built {} mods.", mods.size());
        return mods;
    }

    /**
     * Toggle a mod with the given fileId OR fileName.
     * <p>
     * If the mod is from the modpack distribution, the mod MUST be toggled with the fileId.
     *
     * @param fileId   The modpack distribution fileId. -1 if the mod is not from the pack.
     * @param fileName The mod file name inside the mods folder. Always required.
     */
    public void toggleMod(long fileId, String fileName) {
        InstanceModifications modifications = getOrCreateModifications();
        ModOverride override = modifications.findOverride(fileName);

        Path pathEnabled;
        Path pathDisabled;
        // .disabled in this context can only come from distribution disabled mods.
        if (fileName.endsWith(".disabled")) {
            pathEnabled = path.resolve("mods").resolve(StringUtils.removeEnd(fileName, ".disabled"));
            pathDisabled = path.resolve("mods").resolve(fileName);
        } else {
            pathEnabled = path.resolve("mods").resolve(fileName);
            pathDisabled = path.resolve("mods").resolve(fileName + ".disabled");
        }

        // Override is null, this is a distributed mod, generate override for current state.
        if (override == null) {
            // Could indicate a bug with listing instance mods. But, likely just broken call.
            if (fileId == -1) throw new IllegalArgumentException("Did not find an existing ModOverride for the given name. File ID required.");
            ModpackVersionManifest.ModpackFile file = FastStream.of(versionManifest.getFiles())
                    .filter(e -> e.getId() == fileId)
                    .firstOrDefault();
            if (file == null) throw new IllegalArgumentException("Did not find any files with the given fileId.");

            boolean isEnabled = !fileName.endsWith(".disabled");
            ModOverrideState state = isEnabled ? ModOverrideState.ENABLED : ModOverrideState.DISABLED;
            override = new ModOverride(state, fileName, fileId);
            modifications.getOverrides().add(override);
        }

        try {
            switch (override.getState()) {
                // TODO, remove override if returning mod back to its original state?
                case ENABLED, UPDATED_ENABLED, ADDED_ENABLED -> {
                    Files.move(pathEnabled, pathDisabled);
                }
                case DISABLED, UPDATED_DISABLED, ADDED_DISABLED -> {
                    Files.move(pathDisabled, pathEnabled);
                }
                // We should probably provide a 'restore' endpoint for this.
                case REMOVED -> throw new IllegalArgumentException("Unable to toggle removed mod.");
            }
            override.setState(override.getState().toggle());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to toggle mod.", ex);
        }
        saveModifications();
    }

    public static boolean isMod(String fName) {
        fName = StringUtils.removeEnd(fName, ".disabled");
        return fName.endsWith(".jar") || fName.endsWith(".zip");
    }

    private static long tryGetSize(Path file) {
        try {
            return Files.size(file);
        } catch (IOException ex) {
            return -1;
        }
    }

    public InstanceSnapshot withSnapshot(Consumer<Instance> action) {
        return InstanceSnapshot.create(
                this,
                action
        );
    }

    /**
     * Creates a safe pack name from the instance name, then seeds it with the instance UUID.
     */
    private static String folderNameFor(InstanceJson props) {
        // TODO: Eval the issues cased by this.
//        return props.name
//            .replaceAll("[^a-zA-Z0-9\\s-]", "") + " (" + props.uuid.toString().split("-")[0] + ")";
        return props.uuid.toString();
    }

    // @formatter:off
    public long getId() { return props.id; }
    public long getVersionId() { return props.versionId; }
    public String getName() { return props.name; }
    public Path getDir() { return path; }
    public String getMcVersion() { return props.mcVersion; }
    public UUID getUuid() { return props.uuid; }
    @Deprecated public String getModLoader() { return props.modLoader; }
    public boolean isPendingCloudInstance() { return pendingCloudInstance; }
    // @formatter:on
}
