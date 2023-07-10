package net.creeperhost.creeperlauncher.pack;

import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.covers1624.quack.collection.StreamableIterable;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.platform.OperatingSystem;
import net.creeperhost.creeperlauncher.*;
import net.creeperhost.creeperlauncher.api.data.other.CloseModalData;
import net.creeperhost.creeperlauncher.api.data.other.OpenModalData;
import net.creeperhost.creeperlauncher.api.handlers.ModFile;
import net.creeperhost.creeperlauncher.data.InstanceJson;
import net.creeperhost.creeperlauncher.data.InstanceSupportMeta;
import net.creeperhost.creeperlauncher.data.modpack.ModpackManifest;
import net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest;
import net.creeperhost.creeperlauncher.install.tasks.DownloadTask;
import net.creeperhost.creeperlauncher.install.tasks.NewDownloadTask;
import net.creeperhost.creeperlauncher.minecraft.modloader.forge.ForgeJarModLoader;
import net.creeperhost.creeperlauncher.util.*;
import net.creeperhost.minetogether.lib.cloudsaves.CloudSaveManager;
import net.creeperhost.minetogether.lib.cloudsaves.CloudSyncType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.covers1624.quack.util.SneakyUtils.sneak;
import static net.creeperhost.creeperlauncher.util.MiscUtils.allFutures;

public class Instance implements IPack {

    private static final Logger LOGGER = LogManager.getLogger();

    public final Path path;
    public final InstanceJson props;

    private final InstanceLauncher launcher = new InstanceLauncher(this);
    @Nullable
    public CompletableFuture<?> prepareFuture;
    @Nullable
    public CancellationToken prepareToken;
    private int loadingModPort;
    public ModpackVersionManifest versionManifest;

    private long startTime;

    // Brand-new instance.
    public Instance(ModpackManifest modpack, ModpackVersionManifest versionManifest, boolean isPrivate, byte packType) {
        props = new InstanceJson(modpack, versionManifest, isPrivate, packType);
        path = Settings.getInstancesDir().resolve(folderNameFor(props));
        FileUtils.createDirectories(path);

        this.versionManifest = versionManifest;

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
        if (props.installComplete) {
            Path versionJson = path.resolve("version.json");
            if (Files.exists(versionJson)) {
                versionManifest = JsonUtils.parse(ModpackVersionManifest.GSON, versionJson, ModpackVersionManifest.class);
            } else {
                versionManifest = ModpackVersionManifest.makeInvalid();
            }
        }
    }

    public void importArt(Path file) throws IOException {
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

    /**
     * Force stops the instance.
     * <p>
     * Does not block until the instance has stopped.
     */
    public void forceStop() {
        if (launcher == null) return;
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
        launcher.reset();
        pollVersionManifest();

        InstanceScanner scanner = new InstanceScanner(path, versionManifest);
        scanner.scan();
        if (scanner.isPotentiallyInvalid()) {
            boolean abort = DialogUtil.confirmDialog("Potentially invalid instance", "Abort", "Launch", "Your instance appears to have duplicate mods or invalid scripts.\nIt is highly recommended that you re-install your instance.\nDo you want to abort launching?");
            if (abort) {
                throw new InstanceLaunchException.Abort("Aborted.");
            }
        }

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

        if (!Constants.S3_SECRET.isEmpty() && !Constants.S3_KEY.isEmpty() && !Constants.S3_HOST.isEmpty() && !Constants.S3_BUCKET.isEmpty()) {
            launcher.withStartTask(ctx -> {
                LOGGER.info("Attempting start cloud sync..");
                cloudSync(false);
            });
            launcher.withExitTask(() -> {
                LOGGER.info("Attempting close cloud sync..");
                cloudSync(false);
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
        launcher.launch(token, offlineUsername);
    }

    public InstanceLauncher getLauncher() {
        return launcher;
    }

    public boolean uninstall() throws IOException {
        FileUtils.deleteDirectory(path);
        Instances.refreshInstances();
        return true;
    }

    public boolean browse() throws IOException {
        return browse("");
    }

    public boolean browse(String extraPath) throws IOException {
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

    @Nullable
    public Instance duplicate(String instanceName) throws IOException {
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

    @Override
    public long getId() {
        return props.id;
    }

    @Override
    public String getName() {
        return props.name;
    }

    @Override
    public String getVersion() {
        return props.version;
    }

    @Override
    public Path getDir() {
        return path;
    }

    @Override
    public List<String> getAuthors() {
        return List.of();
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public String getMcVersion() {
        return props.mcVersion;
    }

    @Override
    public String getUrl() {
        return "";
    }

    @Override
    public String getArtURL() {
        return "";
    }

    @Override
    public int getMinMemory() {
        return props.minMemory;
    } // Not needed but oh well, may as well return a value.

    @Override
    public int getRecMemory() {
        return props.recMemory;
    }

    public long getVersionId() {
        return props.versionId;
    }

    public UUID getUuid() {
        return props.uuid;
    }

    public String getModLoader() {
        return props.modLoader;
    }

    public void cloudSync(boolean forceCloud) {
        if (!props.cloudSaves || !Boolean.parseBoolean(Settings.settings.getOrDefault("cloudSaves", "false"))) return;
        OpenModalData.openModal("Please wait", "Checking cloud save synchronization <br>", List.of());

        if (launcher != null || CreeperLauncher.isSyncing.get()) return;

        AtomicInteger progress = new AtomicInteger(0);

        CreeperLauncher.isSyncing.set(true);

        HashMap<String, S3ObjectSummary> s3ObjectSummaries = CloudSaveManager.listObjects(props.uuid.toString());
        AtomicBoolean syncConflict = new AtomicBoolean(false);

        for (S3ObjectSummary s3ObjectSummary : s3ObjectSummaries.values()) {
            Path file = Settings.getInstancesDir().resolve(s3ObjectSummary.getKey());
            LOGGER.debug("{} {}", s3ObjectSummary.getKey(), file.toAbsolutePath());

            if (s3ObjectSummary.getKey().contains("/saves/")) {
                try {
                    CloudSaveManager.downloadFile(s3ObjectSummary.getKey(), file, true, s3ObjectSummary.getETag());
                } catch (Exception e) {
                    syncConflict.set(true);
                    e.printStackTrace();
                    break;
                }
                continue;
            }

            if (Files.notExists(file)) {
                syncConflict.set(true);
                break;
            }
        }

        Runnable fromCloud = () ->
        {
            OpenModalData.openModal("Please wait", "Synchronizing", List.of());

            int localProgress = 0;
            int localTotal = s3ObjectSummaries.size();

            for (S3ObjectSummary s3ObjectSummary : s3ObjectSummaries.values()) {
                localProgress++;

                float percent = Math.round(((float) ((float) localProgress / (float) localTotal) * 100) * 100F) / 100F;

                OpenModalData.openModal("Please wait", "Synchronizing <br>" + percent + "%", List.of());

                if (s3ObjectSummary.getKey().contains(props.uuid.toString())) {
                    Path file = Settings.getInstancesDir().resolve(s3ObjectSummary.getKey());
                    if (Files.notExists(file)) {
                        try {
                            CloudSaveManager.downloadFile(s3ObjectSummary.getKey(), file, true, null);
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                }
            }
            cloudSyncLoop(this.path, false, CloudSyncType.SYNC_MANUAL_SERVER, s3ObjectSummaries);
            syncConflict.set(false);
            Settings.webSocketAPI.sendMessage(new CloseModalData());
        };
        if (forceCloud) {
            fromCloud.run();
        } else if (syncConflict.get()) {
            //Open UI
            OpenModalData.openModal("Cloud Sync Conflict", "We have detected a synchronization error between your saves, How would you like to resolve?", List.of
                    (new OpenModalData.ModalButton("Use Cloud", "green", fromCloud), new OpenModalData.ModalButton("Use Local", "red", () ->
                    {
                        OpenModalData.openModal("Please wait", "Synchronizing", List.of());

                        int localProgress = 0;
                        int localTotal = s3ObjectSummaries.size();

                        for (S3ObjectSummary s3ObjectSummary : s3ObjectSummaries.values()) {
                            localProgress++;

                            float percent = Math.round(((float) ((float) localProgress / (float) localTotal) * 100) * 100F) / 100F;

                            OpenModalData.openModal("Please wait", "Synchronizing <br>" + percent + "%", List.of());

                            Path file = Settings.getInstancesDir().resolve(s3ObjectSummary.getKey());
                            if (Files.notExists(file)) {
                                try {
                                    CloudSaveManager.deleteFile(s3ObjectSummary.getKey());
                                } catch (Exception e) { e.printStackTrace(); }
                            }
                        }
                        cloudSyncLoop(this.path, false, CloudSyncType.SYNC_MANUAL_CLIENT, s3ObjectSummaries);
                        syncConflict.set(false);
                        Settings.webSocketAPI.sendMessage(new CloseModalData());
                    }), new OpenModalData.ModalButton("Ignore", "orange", () ->
                    {
                        props.cloudSaves = false;
                        try {
                            this.saveJson();
                        } catch (IOException e) { e.printStackTrace(); }
                        syncConflict.set(false);
                        Settings.webSocketAPI.sendMessage(new CloseModalData());
                    })));
            while (syncConflict.get()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) { e.printStackTrace(); }
            }
        } else {
            cloudSyncLoop(this.path, false, CloudSyncType.SYNC_NORMAL, s3ObjectSummaries);
            Settings.webSocketAPI.sendMessage(new CloseModalData());
        }
        CreeperLauncher.isSyncing.set(false);
    }

    public void cloudSyncLoop(Path path, boolean ignoreInUse, CloudSyncType cloudSyncType, HashMap<String, S3ObjectSummary> existingObjects) {
        final String host = Constants.S3_HOST;
        final int port = 8080;
        final String accessKeyId = Constants.S3_KEY;
        final String secretAccessKey = Constants.S3_SECRET;
        final String bucketName = Constants.S3_BUCKET;

        Path baseInstancesPath = Settings.getInstancesDir();

        CloudSaveManager.setup(host, port, accessKeyId, secretAccessKey, bucketName);
        if (Files.isDirectory(path)) {
            List<Path> dirContents = FileUtils.listDir(path);
            if (!dirContents.isEmpty()) {
                for (Path innerFile : dirContents) {
                    cloudSyncLoop(innerFile, true, cloudSyncType, existingObjects);
                }
            } else {
                try {
                    //Add a / to allow upload of empty directories
                    CloudSaveManager.syncFile(path, StringUtils.appendIfMissing(CloudSaveManager.fileToLocation(path, baseInstancesPath), "/"), true, existingObjects);
                } catch (Exception e) {
                    LOGGER.error("Upload failed", e);
                }
            }
        } else {
            try {
                LOGGER.debug("Uploading file {}", path.toAbsolutePath());
                switch (cloudSyncType) {
                    case SYNC_NORMAL:
                        try {
                            ArrayList<CompletableFuture<?>> futures = new ArrayList<>();
                            futures.add(CompletableFuture.runAsync(() ->
                            {
                                try {
                                    CloudSaveManager.syncFile(path, CloudSaveManager.fileToLocation(path, baseInstancesPath), true, existingObjects);
                                } catch (Exception e) { e.printStackTrace(); }
                            }, DownloadTask.threadPool));

                            allFutures(futures).join();
                        } catch (Throwable t) {
                            LOGGER.error(t);
                        }
                        break;
                    case SYNC_MANUAL_CLIENT:
                        CloudSaveManager.syncManual(path, CloudSaveManager.fileToLocation(path, Settings.getInstancesDir()), true, true, existingObjects);
                        break;
                    case SYNC_MANUAL_SERVER:
                        CloudSaveManager.syncManual(path, CloudSaveManager.fileToLocation(path, Settings.getInstancesDir()), true, false, existingObjects);
                        break;
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    public List<ModFile> getMods(boolean rich) {
        try {
            Map<String, CurseProps> lookup = rich ? getHashLookup() : Map.of();
            try (Stream<Path> files = Files.walk(path.resolve("mods"))) {
                return files.filter(Files::isRegularFile)
                        .filter(file -> ModFile.isPotentialMod(file.toString()))
                        .map(sneak(path -> {
                            String sha1 = rich ? FileUtils.getHash(path, "SHA-1") : "";
                            CurseProps curseProps = lookup.get(sha1);
                            ModFile modFile = new ModFile(path.getFileName().toString(), "", Files.size(path), sha1).setPath(path);
                            if (curseProps != null) {
                                modFile.setCurseProject(curseProps.curseProject());
                                modFile.setCurseFile(curseProps.curseFile());
                            }

                            return modFile;
                        }))
                        .collect(Collectors.toList());
            }
        } catch (IOException error) {
            LOGGER.log(Level.DEBUG, "Error occurred whilst listing mods on disk", error);
        }

        return new ArrayList<>();
    }

    private Map<String, CurseProps> getHashLookup() throws IOException {
        // This hurts my soul, this system is pending a rewrite tho and this is a 'temporary' fix... Supposedly... Hopefully...
        Map<Long, ModpackVersionManifest.ModpackFile> idLookup = StreamableIterable.of(versionManifest.getFiles())
                .filter(e -> e.getSha1OrNull() != null)
                .toImmutableMap(ModpackVersionManifest.ModpackFile::getId, e -> e);
        String url = Constants.getCreeperhostModpackPrefix(props._private, props.packType) + props.id + "/" + props.versionId + "/mods";
        LOGGER.info("Querying: {}", url);
        String resp = WebUtils.getAPIResponse(url);
        JsonElement jElement = JsonUtils.parseRaw(resp);
        if (!jElement.isJsonObject()) return Map.of();

        JsonObject obj = jElement.getAsJsonObject();
        if (JsonUtils.getString(obj, "status", "error").equalsIgnoreCase("error")) return Map.of();

        if (!obj.has("mods")) return Map.of();
        JsonArray mods = obj.getAsJsonArray("mods");

        Map<String, CurseProps> ret = new HashMap<>();
        for (JsonElement mod : mods) {
            if (!mod.isJsonObject()) continue;
            JsonObject modObject = mod.getAsJsonObject();
            long fileId = JsonUtils.getInt(modObject, "fileId", -1);
            long curseProject = JsonUtils.getInt(modObject, "curseProject", -1);
            long curseFile = JsonUtils.getInt(modObject, "curseFile", -1);
            ModpackVersionManifest.ModpackFile modpackFile = idLookup.get(fileId);
            if (modpackFile != null) {
                ret.put(modpackFile.getSha1().toString(), new CurseProps(curseProject, curseFile));
            }
        }
        return ret;
    }

    public InstanceSnapshot withSnapshot(Consumer<Instance> action) {
        return InstanceSnapshot.create(
                this,
                action
        );
    }

    private static String folderNameFor(InstanceJson props) {
        // TODO, generate folder name from instance name.
        return props.uuid.toString();
    }

    private record CurseProps(long curseProject, long curseFile) { }
}
