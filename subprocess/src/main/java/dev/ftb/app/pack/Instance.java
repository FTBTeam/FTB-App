package dev.ftb.app.pack;

import com.google.gson.JsonParseException;
import dev.ftb.app.Analytics;
import dev.ftb.app.AppMain;
import dev.ftb.app.Instances;
import dev.ftb.app.data.InstanceJson;
import dev.ftb.app.data.InstanceModifications;
import dev.ftb.app.data.InstanceModifications.ModOverride;
import dev.ftb.app.data.InstanceModifications.ModOverrideState;
import dev.ftb.app.data.InstanceSupportMeta;
import dev.ftb.app.data.mod.CurseMetadata;
import dev.ftb.app.data.mod.ModInfo;
import dev.ftb.app.data.modpack.ModpackManifest;
import dev.ftb.app.data.modpack.ModpackVersionManifest;
import dev.ftb.app.data.modpack.ModpackVersionModsManifest;
import dev.ftb.app.install.tasks.DownloadTask;
import dev.ftb.app.instance.InstanceCategory;
import dev.ftb.app.minecraft.modloader.forge.ForgeJarModLoader;
import dev.ftb.app.storage.settings.Settings;
import dev.ftb.app.util.*;
import dev.ftb.app.util.CurseMetadataCache.FileMetadata;
import net.covers1624.quack.collection.ColUtils;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.platform.OperatingSystem;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.BindException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class Instance {
    private static final Set<Long> SPECIAL_LOADER_PACK_IDS = Set.of(
        81L,  // Vanilla
        104L, // Forge
        105L, // Fabric
        116L  // NeoForge
    );
    
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String META_FOLDER_NAME = ".ftbapp";
    
    public static final String MODIFICATIONS_FILE_NAME = "modifications.json";
    public static final String VERSION_MODS_FILE_NAME = "version_mods.json";
    public static final String VERSION_FILE_NAME = "version.json";
    
    public final InstanceArtwork logoArtwork;
    
    public final Path path;
    public final Path metaPath;
    
    public InstanceJson props;
    private @Nullable InstanceModifications modifications;

    private final InstanceLauncher launcher = new InstanceLauncher(this);
    @Nullable
    public CompletableFuture<?> prepareFuture;
    @Nullable
    public CancellationToken prepareToken;
    private int loadingModPort;
    public ModpackVersionManifest versionManifest;

    private long startTime;

    // Brand-new instance.
    public Instance(@Nullable String name, @Nullable String artPath, @Nullable UUID categoryId, ModpackManifest modpack, ModpackVersionManifest versionManifest, String mcVersion, boolean isPrivate, byte packType) {
        props = new InstanceJson(modpack, versionManifest, mcVersion, isPrivate, packType);
        if (name != null) {
            props.name = name;
        }

        props.categoryId = categoryId == null ? InstanceCategory.DEFAULT.uuid() : categoryId;

        path = Settings.getInstancesDir().resolve(folderNameFor(props));
        metaPath = path.resolve(META_FOLDER_NAME);
        
        FileUtils.createDirectories(path);
        FileUtils.createDirectories(metaPath);

        this.versionManifest = versionManifest;
        this.logoArtwork = new InstanceArtwork("logo", metaPath);
        this.storeArtwork(modpack, artPath);

        try {
            saveJson();
        } catch (IOException ex) {
            LOGGER.error("Failed to save instance.", ex);
        }
    }

    // Loading an existing instance.
    public Instance(Path path, Path json) throws IOException {
        this.path = path;
        this.metaPath = path.resolve(META_FOLDER_NAME);
        this.logoArtwork = new InstanceArtwork("logo", metaPath);
        
        props = InstanceJson.load(json);
        loadVersionManifest();
        
        if (Files.notExists(metaPath)) {
            FileUtils.createDirectories(metaPath);
        }
        
        this.tryPutMarkerFile();
    }

    public void tryPutMarkerFile() {
        try {
            Path markerFilePath = this.path.resolve(VERSION_FILE_NAME);
            if (Files.exists(markerFilePath)) {
                // If the marker file already exists, we don't need to do anything.
                return;
            }
            
            Files.writeString(markerFilePath, """
{
    "__comment": "This file is not used by the app anymore as it was moved to the .ftbapp folder, but it is still used as a marker for other things. Please do not delete it.",
}
""");
        } catch (Exception e) {
            LOGGER.error("Failed to put marker file for instance.", e);
        }
    }
    
    private void storeArtwork(ModpackManifest modpack, @Nullable String artPath) {
        // Don't try and resolve a path of a url.
        if (artPath != null && !artPath.startsWith("http://") && !artPath.startsWith("https://")) {
            try {
                updateArtwork(Path.of(artPath));
            } catch (IOException ex) {
                downloadAndUpdateArt(modpack);
            }
            return;
        }

        downloadAndUpdateArt(modpack);
    }

    private void loadVersionManifest() throws IOException {
        if (props.installComplete) {
            Path versionJson = metaPath.resolve(VERSION_FILE_NAME);
            if (Files.exists(versionJson)) {
                versionManifest = JsonUtils.parse(ModpackVersionManifest.GSON, versionJson, ModpackVersionManifest.class);
            } else {
                versionManifest = ModpackVersionManifest.makeInvalid();
            }
            Path modificationsJson = metaPath.resolve(MODIFICATIONS_FILE_NAME);
            if (Files.exists(modificationsJson)) {
                // TODO we need to validate the state of mod modifications.
                //      Dynamically add/remove them as manual modifications are always possible.
                //      We should do this any time the Mods list is queried. I.e the frontend or installer requests it.
                modifications = InstanceModifications.load(modificationsJson);
            }
        }
    }

    public void updateArtwork(Path sourcePath) throws IOException {
        try (var is = Files.newInputStream(sourcePath)) {
            var extension = sourcePath.getFileName().toString();
            if (extension.contains(".")) {
                extension = extension.substring(extension.lastIndexOf(".") + 1);
            }
            
            updateArtwork(is, extension);
        } catch (IOException ex) {
            LOGGER.error("Failed to update artwork.", ex);
            throw ex;
        }
    }
    
    public void updateArtwork(InputStream inputStream, String extension) {
        this.logoArtwork.saveImage(inputStream, extension);
    }
    
    private void downloadAndUpdateArt(ModpackManifest modpack) {
        ModpackManifest.Art art = modpack.getFirstArt("square");
        if (art == null) {
            return;
        }
        
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("art", "");
            Files.delete(tempFile);
            var extension = art.getUrl().substring(art.getUrl().lastIndexOf(".") + 1);
            DownloadTask task = DownloadTask.builder()
                .url(art.getUrl())
                .dest(tempFile)
                .build();
            task.execute(null, null);
            try (InputStream is = Files.newInputStream(tempFile)) {
                updateArtwork(is, extension);
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

    public synchronized void pollVersionManifest() {
        if (props.isImport) return; // Can't update manifests for imports.
        try {
            Pair<ModpackManifest, ModpackVersionManifest> newManifest = ModpackVersionManifest.queryManifests(props.id, props.versionId, props._private, props.packType);
            if (newManifest == null) {
                LOGGER.warn("Failed to update modpack version manifest for instance. This may be a private pack.");
                return;
            }
            versionManifest = newManifest.getRight();
            JsonUtils.write(ModpackVersionManifest.GSON, this.metaPath.resolve(VERSION_FILE_NAME), versionManifest, ModpackVersionManifest.class);
        } catch (IOException ex) {
            LOGGER.warn("Failed to update manifest for modpack. This may be a private pack.", ex);
        }
    }

    // TODO, In theory this meta should be getting added to the regular version manifest.
    //       When that happens we can nuke this.
    public @Nullable ModpackVersionModsManifest getModsManifest() {
        if (this.isCustom()) {
            // We don't need to query the mods manifest for these packs.
            return null;
        }
        
        if (this.props.id == -1 || this.props.versionId == -1) {
            // This is a custom instance, we don't have a modpack ID or version ID.
            return null;
        }
        
        ModpackVersionModsManifest manifest = null;
        Path file = this.metaPath.resolve(VERSION_MODS_FILE_NAME);
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
        if (launcher.isRunning()) {
            throw new InstanceLaunchException("Instance already running.");
        }
        LOGGER.info("Resetting launcher..");
        launcher.reset();
        // TODO, why do we need to do this? Can anything in here change that affects launching? Only the Java versions perhaps?
        LOGGER.info("Polling version manifest.");
        pollVersionManifest();

        LOGGER.info("Scanning instance.");
        InstanceScanner scanner = new InstanceScanner(path, versionManifest, this);
        if (scanner.shouldScan()) {
            scanner.scan();
            if (scanner.isPotentiallyInvalid()) {
                DialogUtil.ConfirmationState abort = DialogUtil.confirmOrIgnore(
                    "Potentially invalid instance", 
                    "Abort", 
                    "Launch",
                    "Don't show again",
                    "Your instance appears to have duplicate mods or invalid scripts.\nIt is highly recommended that you re-install your instance.\nDo you want to abort launching?"
                );
                
                if (abort == DialogUtil.ConfirmationState.CONFIRM) {
                    throw new InstanceLaunchException.Abort("Aborted.");
                } else if (abort == DialogUtil.ConfirmationState.IGNORE) {
                    props.potentiallyBrokenDismissed = true;
                    try {
                        saveJson();
                    } catch (IOException ex) {
                        LOGGER.error("Failed to save instance.", ex);
                    }
                }
            }
        }

        LOGGER.info("Adding start/shutdown tasks..");
        launcher.withStartTask(ctx -> {
            ctx.shellArgs.addAll(MiscUtils.splitCommand(props.shellArgs));
            
            // TODO, `extraArgs` and `jvmArgs` should be an array
            ctx.extraJVMArgs.addAll(MiscUtils.splitCommand(extraArgs));
            
            if (props.programArgs != null && !props.programArgs.isEmpty()) {
                ctx.extraProgramArgs.addAll(MiscUtils.splitCommand(props.programArgs));
            }

            // TODO, do this on LocalInstance load, potentially combine with changes to make jvmArgs an array.
            var args = MiscUtils.splitCommand(props.jvmArgs);
            for (String arg : args) {
                if (arg.contains("-Xmx") || arg.contains("-Xms")) {
                    continue;
                }
                ctx.extraJVMArgs.add(arg);
            }
        });

        if (props.hasInstMods) {
            // TODO, Jar Mods can be done differently
            launcher.withStartTask(ctx -> {
                ForgeJarModLoader.prePlay(this);
            });
        }

        // TODO: Check if private
        launcher.withStartTask(ctx -> {
            Analytics.sendPlayRequest(getId(), getVersionId(), props.packType);
        });

        OperatingSystem os = OperatingSystem.current();
        launcher.withStartTask(ctx -> {
            InstanceSupportMeta supportMeta = InstanceSupportMeta.update();
            if (supportMeta == null) return; // Should be _incredibly_ rare. But just incase...

            List<InstanceSupportMeta.SupportFile> loadingMods = supportMeta.getSupportMods("loading");
            
            // Only inject custom mods if we have any, and we're not a "custom" instance.
            if (!loadingMods.isEmpty() && !this.isCustom() && !this.props.preventMetaModInjection) {
                for (InstanceSupportMeta.SupportFile file : loadingMods) {
                    if (!file.canApply(props.modLoader, os)) continue;
                    file.createTask(path.resolve("mods")).execute(null, null);
                }

                AppMain.closeOldClient();
                loadingModPort = MiscUtils.getRandomEphemeralPort();
                if (loadingModPort != -1) {
                    CompletableFuture.runAsync(() -> {
                        try {
                            AppMain.listenForClient(loadingModPort);
                        } catch (BindException err) {
                            LOGGER.error("Error whilst starting mod socket on port '{}'...", loadingModPort, err);
                        } catch (Exception err) {
                            if (!AppMain.opened) {
                                LOGGER.warn("Error whilst handling message from mod socket - probably nothing!", err);
                                AppMain.opened = false;
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

    public boolean uninstall() {
        com.sun.jna.platform.FileUtils instance = com.sun.jna.platform.FileUtils.getInstance();
        if (!instance.hasTrash()) {
            // Hardcore delete.
            FileUtils.deleteDirectory(path);    
        } else {
            try {
                instance.moveToTrash(path.toFile());
            } catch (IOException ex) {
                LOGGER.error("Failed to move instance to trash.", ex);
                FileUtils.deleteDirectory(path); // Just delete it instead.
            }
        }
        
        Instances.refreshInstances();
        return true;
    }

    public boolean browse() {
        return browse("");
    }
    
    public boolean browse(String extraPath) {
        return FileUtils.openFolder(path.resolve(extraPath));
    }

    public void setModified(boolean state) {
        props.isModified = state;
    }

    public void saveJson() throws IOException {
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
                Path modificationsJson = this.metaPath.resolve(MODIFICATIONS_FILE_NAME);
                InstanceModifications.save(modificationsJson, modifications);
            }
        } catch (IOException ex) {
            LOGGER.error("Failed to save instance modifications.", ex);
        }
    }

    @Nullable
    public Instance duplicate(String instanceName, @Nullable UUID category) throws IOException {
        InstanceJson json = new InstanceJson(props, UUID.randomUUID(), !instanceName.isEmpty() ? instanceName : props.name);
        json.totalPlayTime = 0;
        json.lastPlayed = 0;
        json.potentiallyBrokenDismissed = false;
        json.categoryId = category != null ? category : props.categoryId;

        Path newDir = Settings.getInstancesDir().resolve(folderNameFor(json));
        Path newJson = newDir.resolve("instance.json");

        Files.createDirectories(newDir);

        org.apache.commons.io.FileUtils.copyDirectory(path.toFile(), newDir.toFile());
        InstanceJson.save(newJson, json);

        return new Instance(newDir, newJson);
    }

    /**
     * Get the mods list.
     *
     * @param rich If rich data is required up front.
     * @return The mods.
     */
    public synchronized List<ModInfo> getMods(boolean rich) {
        LOGGER.info("Building instance mods list..");
        List<ModInfo> mods = new ArrayList<>();

        ModpackVersionModsManifest modsManifest = rich ? getModsManifest() : null;
        InstanceModifications modifications = getModifications();

        Path modsDir = path.resolve("mods");

        // Populate all mods from the regular version manifest.
        for (ModpackVersionManifest.ModpackFile file : versionManifest.getFiles()) {
            if (!file.getPath().startsWith("./mods") || !isMod(file.getName())) continue;

            String sha1 = Objects.toString(file.getSha1OrNull(), null);
            long murmur = -1;
            if (file.getHashesOrNull() != null) {
                murmur = file.getHashesOrNull().cfMurmur;
            }

            ModOverride override = modifications != null ? modifications.findOverride(file.getId()) : null;
            ModpackVersionModsManifest.Mod mod = modsManifest != null ? modsManifest.getMod(file.getId()) : null;

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
                    String.valueOf(murmur),
                    rich ? CurseMetadataCache.get().getCurseMeta(mod, String.valueOf(murmur)) : null
            ));
        }

        // Mods added manually or via CurseForge integ.
        if (modifications != null) {
            for (ModOverride override : modifications.getOverrides()) {
                // -1 is for non-pack overrides.
                if (!override.getState().added() && !override.getState().updated()) continue;

                Path file = modsDir.resolve(override.getFileName());
                long murmurHash = -1;
                if (!Files.exists(file)) {
                    // If t he file does not exist, it might be disabled, let's try that one as well
                    file = modsDir.resolve(override.getFileName() + ".disabled");
                    if (!Files.exists(file)) {
                        file = null;
                    }
                }

                if (file != null) {
                    try {
                        var fileBytes = Files.readAllBytes(file);
                        murmurHash = HashingUtils.createCurseForgeMurmurHash(fileBytes);
                    } catch (IOException ex) {
                        LOGGER.error("Error reading file for override: {}. Unable to process this whilst generating mods list.", override.getFileName(), ex);
                    }
                }
                
                CurseMetadata ids;
                if (rich) {
                    ids = CurseMetadataCache.get().getCurseMeta(override.getCurseProject(), override.getCurseFile());
                } else {
                    ids = CurseMetadata.basic(override.getCurseProject(), override.getCurseFile());
                }
                mods.add(new ModInfo(
                        -1,
                        override.getFileName(),
                        null,
                        override.getState().enabled(),
                        tryGetSize(file),
                        override.getSha1(),
                        String.valueOf(murmurHash),
                        ids
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

            String murmurHash;
            String sha1;
            long size;
            try {
                size = Files.size(path);
                var fileBytes = Files.readAllBytes(path);     
                
                sha1 = DigestUtils.sha1Hex(fileBytes);
                murmurHash = String.valueOf(HashingUtils.createCurseForgeMurmurHash(fileBytes));

            } catch (IOException ex) {
                LOGGER.error("Error reading file. Unable to process this whilst generating mods list.", ex);
                continue;
            }

            long curseProject = -1;
            long curseFile = -1;
            FileMetadata metadata = CurseMetadataCache.get().queryMetadata(murmurHash);
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
                    murmurHash,
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
        } catch (Exception ex) {
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
     * Checks if this instance is a loader instance. Aka: The instance is a vanilla, forge, fabric, or neo instance.
     */
    public boolean isCustom() {
        if (this.props.id == 0 || this.props.versionId == 0 || this.props.id == -1 || this.props.versionId == -1) {
            return false;
        }

        return SPECIAL_LOADER_PACK_IDS.contains(this.props.id);
    }

    /**
     * Creates a safe pack name from the instance name
     */
    private static String folderNameFor(InstanceJson props) {
        var computedName = props.name.replaceAll("[^a-zA-Z0-9\\s-]", "").toLowerCase();
        
        // Trim the computed name to 64 characters.
        computedName = computedName.substring(0, Math.min(computedName.length(), 64));
        computedName = computedName.trim();
        
        Path instancesDir = Settings.getInstancesDir();
        if (!Files.exists(instancesDir.resolve(computedName))) {
            return computedName;
        }
        
        int count = 1;
        try (var stream = Files.list(instancesDir)) {
            for (var path : stream.toList()) {
                if (path.getFileName().toString().toLowerCase().startsWith(computedName)) {
                    count++;
                }
            }
        } catch (IOException ex) {
            LOGGER.error("Failed to count instances.", ex);
            
            // Fallback to UUID.
            return computedName + "-" + UUID.randomUUID().toString().substring(0, 8).trim();
        }
        
        return computedName + " (" + count + ")".trim();
    }

    @Nullable
    public String resolveBasicModLoader() {
        var modloader = this.props.modLoader.toLowerCase();
        if (modloader.isEmpty()) {
            return null;
        }
        
        if (modloader.contains("neoforge")) {
            return "neoforge";
        } else if (modloader.contains("forge")) {
            return "forge";
        } else if (modloader.contains("fabric")) {
            return "fabric";
        } else if (modloader.contains("quilt")) {
            return "quilt";
        } else {
            return null;
        }
    }

    // @formatter:off
    public long getId() { return props.id; }
    public long getVersionId() { return props.versionId; }
    public String getName() { return props.name; }
    public Path getDir() { return path; }
    public String getMcVersion() { return props.mcVersion; }
    public UUID getUuid() { return props.uuid; }
    @Deprecated public String getModLoader() { return props.modLoader; }
    // @formatter:on
}
