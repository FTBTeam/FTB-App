package net.creeperhost.creeperlauncher.pack;

import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import net.covers1624.quack.gson.PathTypeAdapter;
import net.creeperhost.creeperlauncher.api.data.other.CloseModalData;
import net.creeperhost.creeperlauncher.api.data.other.OpenModalData;
import net.creeperhost.creeperlauncher.api.handlers.ModFile;
import net.creeperhost.creeperlauncher.minecraft.modloader.forge.ForgeJarModLoader;
import net.creeperhost.minetogether.lib.cloudsaves.CloudSaveManager;
import net.creeperhost.minetogether.lib.cloudsaves.CloudSyncType;
import net.creeperhost.creeperlauncher.install.tasks.DownloadTask;
import net.creeperhost.creeperlauncher.os.OS;
import net.creeperhost.creeperlauncher.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;

import net.creeperhost.creeperlauncher.*;
import net.creeperhost.creeperlauncher.api.DownloadableFile;
import net.creeperhost.creeperlauncher.install.tasks.FTBModPackInstallerTask;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.BindException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static net.creeperhost.creeperlauncher.util.MiscUtils.allFutures;

//TODO: Turn LocalInstance into a package somehow and split out the individual parts for easier navigation.
//TODO: Switch prePlay events, postInstall(Semi-Completed), events etc into the same setup as gameClose to allow multiple code blocks.
public class LocalInstance implements IPack
{
    private static final Logger LOGGER = LogManager.getLogger();
    public boolean _private;

    private UUID uuid;
    private long id;
    private String art;
    @JsonAdapter(PathTypeAdapter.class)
    private Path path;
    private long versionId;
    public String name;
    private int minMemory = 2048;
    private int recMemory = 4096;
    public int memory = Integer.parseInt(Settings.settings.getOrDefault("memory", "2048"));
    private String version;
    @JsonAdapter(PathTypeAdapter.class)
    private Path dir;
    private List<String> authors;
    private String description;
    public String mcVersion;
    public String jvmArgs = Settings.settings.getOrDefault("jvmArgs", "");
    public boolean embeddedJre = Boolean.parseBoolean(Settings.settings.getOrDefault("embeddedJre", "true"));
    @JsonAdapter(PathTypeAdapter.class)
    public Path jrePath = Settings.getPathOpt("jrepath", null);
    private String url;
    private String artUrl;
    public int width = Integer.parseInt(Settings.settings.getOrDefault("width", String.valueOf((int) Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 2)));
    public int height = Integer.parseInt(Settings.settings.getOrDefault("height", String.valueOf((int) Toolkit.getDefaultToolkit().getScreenSize().getHeight() / 2)));
    public String modLoader = "";
    private boolean isModified = false;
    private boolean isImport = false;
    public boolean cloudSaves = false;
    public boolean hasInstMods = false;
    public boolean installComplete = true;
    public byte packType;

    /**
     * The current play time in millis.
     */
    public long totalPlayTime;

    private transient InstanceLauncher launcher = new InstanceLauncher(this);
    private transient int loadingModPort;
    public transient boolean hasLoadingMod;
    public transient boolean hasL4jPatcher;
    private transient Runnable preUninstall;
    private transient boolean preUninstallAsync;
    private transient AtomicBoolean inUse = new AtomicBoolean(false);
    private transient HashMap<String, instanceEvent> gameCloseEvents = new HashMap<>();
    public transient ModPack manifest;
    private transient boolean updateManifest = false;

    private transient long startTime;

    public LocalInstance(ModPack pack, long versionId)
    {
        //We're making an instance!
        String tmpArt = "";
        UUID uuid = UUID.randomUUID();
        this.uuid = uuid;
        this.manifest = pack;
        this.versionId = versionId;
        this.path = Settings.getInstanceLocOr(Constants.INSTANCES_FOLDER_LOC).resolve(this.uuid.toString());
        this.cloudSaves = Boolean.getBoolean(Settings.settings.getOrDefault("cloudSaves", "false"));
        this.name = pack.getName();
        this.version = pack.getVersion();
        this.dir = this.path;
        this.authors = pack.getAuthors();
        this.description = pack.getDescription();
        this.mcVersion = pack.getMcVersion();
        this.url = pack.getUrl();
        this.artUrl = pack.getArtURL();
        this.id = pack.getId();
        if (Settings.settings.containsKey("jvmargs"))
        {
            this.jvmArgs = Settings.settings.get("jvmargs");
        }
        this.recMemory = pack.getRecMemory();
        this.minMemory = pack.getMinMemory();
        this.memory = this.recMemory;
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        long totalMemory = hal.getMemory().getTotal() / 1024 / 1024;
        if(this.recMemory > (totalMemory-2048))
        {
            this.memory = this.minMemory;
        }
        FileUtils.createDirectories(path);
        Path artFile = path.resolve("art.png");
        if (Files.notExists(artFile))
        {
            try
            {
                if (ForgeUtils.isUrlValid(this.getArtURL()))
                {
                    DownloadUtils.downloadFile(artFile, this.getArtURL());
                } else
                {
                    LOGGER.error("The url '{}' is not valid.", getArtURL());
                }
            } catch (Exception err)
            {
                LOGGER.error("Unable to download and save artwork.", err);
            }
        }
        try
        {
            Base64.Encoder en = Base64.getEncoder();
            tmpArt = "data:image/png;base64," + en.encodeToString(Files.readAllBytes(artFile));
        } catch (Exception err)
        {
            LOGGER.error("Unable to encode artwork for embedding.", err);
        }
        this.art = tmpArt;
        try
        {
            this.saveJson();
        } catch (Exception err)
        {
            LOGGER.error("Unable to write instance configuration.", err);
        }
    }

    public LocalInstance(ModPack pack, long versionId, boolean _private, byte packType)
    {
        //We're making an instance!
        String tmpArt = "";
        UUID uuid = UUID.randomUUID();
        this.uuid = uuid;
        this.manifest = pack;
        this.versionId = versionId;
        this.path = Settings.getInstanceLocOr(Constants.INSTANCES_FOLDER_LOC).resolve(this.uuid.toString());
        this.cloudSaves = Boolean.getBoolean(Settings.settings.getOrDefault("cloudSaves", "false"));
        this.name = pack.getName();
        this.version = pack.getVersion();
        this.dir = this.path;
        this.authors = pack.getAuthors();
        this.description = pack.getDescription();
        this.mcVersion = pack.getMcVersion();
        this.url = pack.getUrl();
        this.artUrl = pack.getArtURL();
        this.id = pack.getId();
        this.packType = packType;
        this._private = _private;
        if (Settings.settings.containsKey("jvmargs"))
        {
            this.jvmArgs = Settings.settings.get("jvmargs");
        }
        this.recMemory = pack.getRecMemory();
        this.minMemory = pack.getMinMemory();
        this.memory = this.recMemory;
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();
        long totalMemory = hal.getMemory().getTotal() / 1024 / 1024;
        if(this.recMemory > (totalMemory-2048))
        {
            this.memory = this.minMemory;
        }
        FileUtils.createDirectories(path);
        Path artFile = path.resolve("art.png");
        if (Files.notExists(artFile))
        {
            try
            {
                if (ForgeUtils.isUrlValid(this.getArtURL()))
                {
                    DownloadUtils.downloadFile(artFile, this.getArtURL());
                } else
                {
                    LOGGER.error("The url '{}' is not valid.", getArtURL());
                }
            } catch (Exception err)
            {
                LOGGER.error("Unable to download and save artwork.", err);
            }
        }
        try
        {
            Base64.Encoder en = Base64.getEncoder();
            //Resize the image, 1024x1024 is gonna cause issues with OOM's on poorer systems and just isn't used.
            BufferedImage manipulatedArt = ImageUtils.resizeImage(Files.readAllBytes(artFile), 256, 256);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(manipulatedArt, "png", baos);
            tmpArt = "data:image/png;base64," + en.encodeToString(baos.toByteArray());
            //Replace "art.png" with "folder.jpg", smaller and will show as a preview folder art like album art on windows. Should make identifying instances a bit easier and reduce file size.
            Path folderArt = path.resolve("folder.jpg");
            baos.reset();
            ImageIO.write(manipulatedArt, "jpg", baos);
            Files.write(folderArt, baos.toByteArray());
            Files.delete(artFile);
        } catch (Exception err)
        {
            LOGGER.error("Unable to encode artwork for embedding.", err);
        }
        this.art = tmpArt;
        try
        {
            this.saveJson();
        } catch (Exception err)
        {
            LOGGER.error("Unable to write instance configuration.", err);
        }
    }

    public LocalInstance(Path path) throws FileNotFoundException
    {
        //We're loading an existing instance
        this.path = path;
        this.uuid = UUID.fromString(path.getFileName().toString());//TODO, this should not parse its uuid from the file name.
        Path json = path.resolve("instance.json");
        if (Files.notExists(json)) throw new FileNotFoundException("Instance does not exist!");

        try (BufferedReader reader = Files.newBufferedReader(json)) {
            LocalInstance jsonOutput = GsonUtils.GSON.fromJson(reader, LocalInstance.class);
            this.id = jsonOutput.id;
            this.name = jsonOutput.name;
            this.artUrl = jsonOutput.artUrl;
            this.mcVersion = jsonOutput.mcVersion;
            this.authors = jsonOutput.authors;
            this.art = jsonOutput.art;
            this.memory = jsonOutput.memory;
            this.version = jsonOutput.version;
            this.versionId = jsonOutput.versionId;
            this.width = jsonOutput.width;
            this.height = jsonOutput.height;
            this.url = jsonOutput.url;
            this.minMemory = jsonOutput.minMemory;
            this.recMemory = jsonOutput.recMemory;
            this.jvmArgs = jsonOutput.jvmArgs;
            this.modLoader = jsonOutput.modLoader;
            if((this.modLoader == null || this.modLoader.isEmpty()) && (this.mcVersion == null || this.mcVersion.isEmpty())) this.modLoader = jsonOutput.getVersion();
            this.jrePath = jsonOutput.jrePath;
            this.dir = this.path;
            this.cloudSaves = jsonOutput.cloudSaves;
            this.hasInstMods = jsonOutput.hasInstMods;
            this.packType = jsonOutput.packType;
            if(Files.exists(path.resolve("modpack.json")))
            {
                this.manifest = FTBModPackInstallerTask.getPackFromFile(path);

//                try (BufferedReader manifestreader = Files.newBufferedReader(path.resolve("modpack.json"))) {
//                    ModPack pack = GsonUtils.GSON.fromJson(manifestreader, ModPack.class);
//                    this.manifest = pack;
//                }
            }
            this._private = jsonOutput._private;
            this.installComplete = jsonOutput.installComplete;
            reader.close();
        } catch(Exception e)
        {
            LOGGER.error(e);
            throw new RuntimeException("Instance is corrupted!", e);
        }
    }

    public LocalInstance(String LauncherImportPath, Boolean isTwitch)
    {
        //Import a pack from the FTB or Twitch local packs
        UUID uuid = UUID.randomUUID();
        this.uuid = uuid;
        this.isImport = true;
        this.path = Settings.getInstanceLocOr(Constants.INSTANCES_FOLDER_LOC).resolve(uuid.toString());
        //this.hasLoadingMod = checkForLaunchMod();
    }

    private static final String[] candidates = new String[] {
            "net/creeperhost/traylauncher/TrayLauncher.class",
            "net/creeperhost/launchertray/LauncherTray.class",
            "net/creeperhost/launchertray/transformer/HookLoader.class"
    };

    private boolean checkForLaunchMod() {
        Path modsDir = path.resolve("mods");
        if (!Files.exists(modsDir)) {
            return false;
        }
        ElapsedTimer timer = new ElapsedTimer();

        LOGGER.info("Checking for Launch Mod for instance {}}({})..", uuid, name);
        boolean ret = FileUtils.listDir(modsDir).parallelStream()
                .anyMatch(file -> {
                    if (!Files.isRegularFile(file)) return false;

                    try (InputStream fileStream = Files.newInputStream(file); ZipInputStream zin = new ZipInputStream(fileStream)) {
                        Set<String> entries = new HashSet<>();

                        ZipEntry entry;
                        while ((entry = zin.getNextEntry()) != null) {
                            entries.add(entry.getName());
                        }

                        for (String candidate : candidates) {
                            if (entries.contains(candidate)) {
                                return true;
                            }
                        }
                    } catch (IOException ignored) {
                    }
                    return false;
                });
        String s = ret ? "Found" : "Didn't find";
        LOGGER.info("{} Launch Mod for instance {}({}) in {}.", s, uuid, name, timer.elapsedStr());
        return ret;
    }

    private LocalInstance()
    {
    }

    public FTBModPackInstallerTask install()
    {
        // Can't reinstall an import...
        //Download everything! wget --mirror http://thewholeinternet.kthxbai
        FTBModPackInstallerTask installer = new FTBModPackInstallerTask(this);
        if (!this.isImport)
        {
            installComplete = false;
            try {
                saveJson(); // save to ensure saved to disk as false
            } catch (IOException e) {
            }
            CreeperLauncher.isInstalling.set(true);
            Analytics.sendInstallRequest(this.getId(), this.getVersionId(), this.packType);
            LOGGER.debug("Running installer async task");
            installer.execute().thenRunAsync(() ->
            {
                LOGGER.debug("Running after installer task");
                FTBModPackInstallerTask.currentStage = FTBModPackInstallerTask.Stage.FINISHED;
                CreeperLauncher.isInstalling.set(false);
                try
                {
                    installComplete = true;
                    this.saveJson();
                } catch (IOException e) { e.printStackTrace(); }
                this.hasLoadingMod = checkForLaunchMod();
            });
        }
        return installer;
    }

    public FTBModPackInstallerTask update(long versionId)
    {
        this.versionId = versionId;

        FTBModPackInstallerTask update = new FTBModPackInstallerTask(this);

        try {
            List<DownloadableFile> requiredDownloads = update.getRequiredDownloads(path.resolve("version.json"), null);
            requiredDownloads.forEach(e -> {
                try {
                    Files.delete(e.getPath());
                } catch (IOException ignored) {
                }
            });
        } catch (MalformedURLException e) {
            // fall back to old delete
            Path mods = path.resolve("mods");
            Path coremods = path.resolve("coremods");
            Path instmods = path.resolve("instmods");

            Path config = path.resolve("config");
            Path resources = path.resolve("resources");
            Path scripts = path.resolve("scripts");

            FileUtils.deleteDirectory(mods);
            FileUtils.deleteDirectory(coremods);
            FileUtils.deleteDirectory(instmods);
            FileUtils.deleteDirectory(config);
            FileUtils.deleteDirectory(resources);
            FileUtils.deleteDirectory(scripts);
        }
        this.hasLoadingMod = checkForLaunchMod();
        update.execute().thenRunAsync(() ->
        {
            this.updateVersionFromFile();
            try {
                this.saveJson();
            } catch (IOException ignored){}
            FTBModPackInstallerTask.currentStage = FTBModPackInstallerTask.Stage.FINISHED;
            CreeperLauncher.isInstalling.set(false);
            try
            {
                this.saveJson();
            } catch (IOException e) { e.printStackTrace(); }
        });
        return update;
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
     * @param requestId Websocket request id, used for progress callbacks.
     * @param extraArgs Extra JVM arguments.
     * @param loadInApp If 'launch mod' should be used.
     * @throws InstanceLaunchException If there was an error preparing or starting the instance.
     */
    public void play(int requestId, String extraArgs, boolean loadInApp) throws InstanceLaunchException {
        if (launcher.isRunning()) {
            throw new InstanceLaunchException("Instance already running.");
        }
        launcher.reset();

        launcher.withStartTask(ctx -> {
            // TODO, split on space. Needs to support escaping. `extraArgs` should be an array
            Collections.addAll(ctx.extraJVMArgs, extraArgs.split(" "));
        });

        launcher.withStartTask(ctx -> {
            DownloadUtils.downloadFile(dir.resolve("Log4jPatcher-1.0.1.jar"), Constants.LOG4JPATCHER_URL);
            ctx.extraJVMArgs.add("-javaagent:Log4jPatcher-1.0.1.jar");
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

        if (hasInstMods) {
            // TODO, Jar Mods can be done differently
            launcher.withStartTask(ctx -> {
                ForgeJarModLoader.prePlay(this);
            });
        }

        launcher.withStartTask(ctx -> {
            Analytics.sendPlayRequest(getId(), getVersionId(), packType);
        });

        this.hasLoadingMod = checkForLaunchMod();
        //TODO this is to patch log4j
        Path log4jPatcher = dir.resolve("Log4jPatcher-1.0.0.jar");
        this.hasL4jPatcher = Files.exists(log4jPatcher);
        if(!hasL4jPatcher)
        {
            LOGGER.warn("Log4jPatcher not found, Attempting to download");
            try
            {
              Files.createDirectories(log4jPatcher.getParent());
            } 
            catch (IOException e) {e.printStackTrace();}

            boolean downloaded = DownloadUtils.downloadFile(log4jPatcher, "https://media.forgecdn.net/files/3557/251/Log4jPatcher-1.0.0.jar");
            if(downloaded)
            {
              LOGGER.info("Log4jPatcher successfully downloaded to " + log4jPatcher);
              hasL4jPatcher = true;
            }
        }
      
        //TODO: THIS IS FOR TESTING ONLY, PLEASE REMOVE ME IN FUTURE
        if(OS.CURRENT == OS.WIN && loadInApp)
        {
            if (!this.hasLoadingMod) {
                if (modLoader.startsWith("1.7.10")) {
                    DownloadUtils.downloadFile(dir.resolve("mods/launchertray-1.0.jar"), "https://dist.creeper.host/modpacks/maven/com/sun/jna/1.7.10-1.0.0/d4c2da853f1dbc80ab15b128701001fd3af6718f");
                    this.hasLoadingMod = checkForLaunchMod();
                } else if (modLoader.startsWith("1.12.2")) {
                    DownloadUtils.downloadFile(dir.resolve("mods/launchertray-1.0.jar"), "https://dist.creeper.host/modpacks/maven/net/creeperhost/launchertray/transformer/1.0/381778e244181cc2bb7dd02f03fb745164e87ee0");
                    this.hasLoadingMod = checkForLaunchMod();
                } else if (modLoader.startsWith("1.15") || modLoader.startsWith("1.16")) {
                    DownloadUtils.downloadFile(dir.resolve("mods/launchertray-1.0.jar"), "https://dist.creeper.host/modpacks/maven/net/creeperhost/traylauncher/1.0/134dd1944e04224ce53ff18750e81f5517704c8e");
                    DownloadUtils.downloadFile(dir.resolve("mods/launchertray-progress-1.0.jar"), "https://dist.creeper.host/modpacks/maven/net/creeperhost/traylauncher/unknown/74ced30ca35e88b583969b6d74efa0f7c2470e8b");
                    this.hasLoadingMod = checkForLaunchMod();
                }
            }
            launcher.withStartTask(ctx -> {
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
                    ctx.extraJVMArgs.add("-Dchtray.instance=" + uuid.toString());
                } else {
                    LOGGER.warn("Failed to find free Ephemeral port.");
                }
            });
        }

        if (CreeperLauncher.mtConnect != null && CreeperLauncher.mtConnect.isEnabled()) {
            launcher.withStartTask(ctx -> {
                LOGGER.info("Starting MineTogether connect..");
                CreeperLauncher.mtConnect.connect();
            });
            launcher.withExitTask(() -> {
                CreeperLauncher.mtConnect.disconnect();
            });
        }
        launcher.withStartTask(ctx -> {
            startTime = System.currentTimeMillis();
        });
        launcher.withExitTask(() -> {
            long endTime = System.currentTimeMillis();
            totalPlayTime += endTime - startTime;
            saveJson();
        });
        launcher.launch(requestId);
    }

    public boolean uninstall() throws IOException
    {
        FileUtils.deleteDirectory(path);
        Instances.refreshInstances();
        return true;
    }

    public LocalInstance clone()
    {
        //Clone the instance on the file system and return the new instance
        return this;
    }

    public boolean browse() throws IOException
    {
        if (Desktop.isDesktopSupported())
        {
            Desktop.getDesktop().open(path.toFile());
            return true;
        }
        return false;
    }
    public void setModified(boolean state)
    {
        this.isModified = state;
    }
    public boolean saveJson() throws IOException
    {
        try (BufferedWriter writer = Files.newBufferedWriter(path.resolve("instance.json"))) {
            GsonUtils.GSON.toJson(this, writer);
        }
        if(updateManifest)
        {
            try (BufferedWriter writer = Files.newBufferedWriter(path.resolve("modpack.json"))) {
                GsonUtils.GSON.toJson(this.manifest, writer);
                writer.close();
                updateManifest = false;
            }
        }
        return true;
    }

    @Override
    public long getId()
    {
        return id;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getVersion()
    {
        return version;
    }

    private void updateVersionFromFile() {
        try(BufferedReader reader = Files.newBufferedReader(getDir().resolve("version.json"))) {
            JsonObject version = GsonUtils.GSON.fromJson(reader, JsonObject.class);
            this.version = version.get("name").getAsString();
        } catch (IOException e) {
            LOGGER.error("Failed to read version json.", e);
        }
    }
    @Override
    public Path getDir()
    {
        return dir;
    }

    @Override
    public List<String> getAuthors()
    {
        return authors;
    }

    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
    public String getMcVersion()
    {
        return mcVersion;
    }

    @Override
    public String getUrl()
    {
        return url;
    }

    @Override
    public String getArtURL()
    {
        return artUrl;
    }

    @Override
    public int getMinMemory()
    {
        return minMemory;
    } // Not needed but oh well, may as well return a value.

    @Override
    public int getRecMemory()
    {
        return recMemory;
    }

    public long getVersionId()
    {
        return versionId;
    }

    public UUID getUuid()
    {
        return uuid;
    }

    public String getModLoader()
    {
        return modLoader;
    }

    public ModPack getManifest(Runnable updateCallback)
    {
        AtomicReference<ModPack> newManifest = new AtomicReference<>();
        CompletableFuture ftr = CompletableFuture.runAsync(() -> {
            ModPack oldManifest = this.manifest;//Copy of existing manifest
            ModPack pack = FTBModPackInstallerTask.getPackFromAPI(this.id, this.versionId, this._private, this.packType);
            if (pack == null) {
                pack = FTBModPackInstallerTask.getPackFromAPI(this.id, this.versionId, !this._private, this.packType);
            }
            if(pack != null) {
                newManifest.set(pack);
                if (oldManifest != null && (!newManifest.get().equals(oldManifest))) {
                    //Manifest has changed server side from cache
                    this.manifest = newManifest.get();
                    this.updateManifest = true;
                    if (updateCallback != null) updateCallback.run();
                }
            }
        });
        if(this.manifest == null)
        {
            ftr.join();
            this.manifest = newManifest.get();
            try {
                saveJson();
            } catch (IOException ignored) { }//TODO: We should really stop ignoring saving errors
        }
        return this.manifest;
    }

    public boolean setJre(boolean autoDetect, Path path)
    {
        Path javaExec = null;
        String javaBinary = "javaw.exe";
        switch (OS.CURRENT)
        {
            case LINUX:
            case MAC:
                javaBinary = "java";
                break;
        }
        if (autoDetect)
        {
            if (!this.embeddedJre)
            {
                javaExec = Paths.get(System.getProperty("java.home")).resolve("bin").resolve(javaBinary);
            }
        } else
        {
            javaExec = path.resolve(javaBinary);
        }
        if (javaExec != null && Files.exists(javaExec))
        {
            jrePath = javaExec.toAbsolutePath();
        } else
        {
            embeddedJre = true;
            return false;
        }
        return true;
    }

    public void cloudSync(boolean forceCloud)
    {
        if(!cloudSaves || !Boolean.parseBoolean(Settings.settings.getOrDefault("cloudSaves", "false"))) return;
        OpenModalData.openModal("Please wait", "Checking cloud save synchronization <br>", List.of());

        if (launcher != null || CreeperLauncher.isSyncing.get()) return;

        AtomicInteger progress = new AtomicInteger(0);

        CreeperLauncher.isSyncing.set(true);

        HashMap<String, S3ObjectSummary> s3ObjectSummaries = CloudSaveManager.listObjects(this.uuid.toString());
        AtomicBoolean syncConflict = new AtomicBoolean(false);

        for(S3ObjectSummary s3ObjectSummary : s3ObjectSummaries.values())
        {
            Path file = Settings.getInstanceLocOr(Constants.INSTANCES_FOLDER_LOC).resolve(s3ObjectSummary.getKey());
            LOGGER.debug("{} {}", s3ObjectSummary.getKey(),file.toAbsolutePath());

            if(s3ObjectSummary.getKey().contains("/saves/"))
            {
                try
                {
                    CloudSaveManager.downloadFile(s3ObjectSummary.getKey(), file, true, s3ObjectSummary.getETag());
                } catch (Exception e)
                {
                    syncConflict.set(true);
                    e.printStackTrace();
                    break;
                }
                continue;
            }

            if(Files.notExists(file))
            {
                syncConflict.set(true);
                break;
            }
        }

        Runnable fromCloud = () ->
        {
            OpenModalData.openModal("Please wait", "Synchronizing", List.of());

            int localProgress = 0;
            int localTotal = s3ObjectSummaries.size();

            for(S3ObjectSummary s3ObjectSummary : s3ObjectSummaries.values())
            {
                localProgress++;

                float percent = Math.round(((float)((float)localProgress / (float)localTotal) * 100) * 100F) / 100F;

                OpenModalData.openModal("Please wait", "Synchronizing <br>" + percent + "%", List.of());

                if(s3ObjectSummary.getKey().contains(this.uuid.toString()))
                {
                    Path file = Settings.getInstanceLocOr(Constants.INSTANCES_FOLDER_LOC).resolve(s3ObjectSummary.getKey());
                    if(Files.notExists(file))
                    {
                        try
                        {
                            CloudSaveManager.downloadFile(s3ObjectSummary.getKey(), file, true, null);
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                }}
            cloudSyncLoop(this.path, false, CloudSyncType.SYNC_MANUAL_SERVER, s3ObjectSummaries);
            syncConflict.set(false);
            Settings.webSocketAPI.sendMessage(new CloseModalData());
        };
        if(forceCloud)
        {
            fromCloud.run();
        }
        else if(syncConflict.get())
        {
            //Open UI
            OpenModalData.openModal("Cloud Sync Conflict", "We have detected a synchronization error between your saves, How would you like to resolve?", List.of
            ( new OpenModalData.ModalButton("Use Cloud", "green", fromCloud), new OpenModalData.ModalButton("Use Local", "red", () ->
            {
                OpenModalData.openModal("Please wait", "Synchronizing", List.of());

                int localProgress = 0;
                int localTotal = s3ObjectSummaries.size();

                for(S3ObjectSummary s3ObjectSummary : s3ObjectSummaries.values())
                {
                    localProgress++;

                    float percent = Math.round(((float)((float)localProgress / (float)localTotal) * 100) * 100F) / 100F;

                    OpenModalData.openModal("Please wait", "Synchronizing <br>" + percent + "%", List.of());

                    Path file = Settings.getInstanceLocOr(Constants.INSTANCES_FOLDER_LOC).resolve(s3ObjectSummary.getKey());
                    if (Files.notExists(file))
                    {
                        try
                        {
                            CloudSaveManager.deleteFile(s3ObjectSummary.getKey());
                        } catch (Exception e) { e.printStackTrace(); }
                    }
                }
                cloudSyncLoop(this.path, false, CloudSyncType.SYNC_MANUAL_CLIENT, s3ObjectSummaries);
                syncConflict.set(false);
                Settings.webSocketAPI.sendMessage(new CloseModalData());
            }), new OpenModalData.ModalButton("Ignore", "orange", () ->
            {
                cloudSaves = false;
                try {
                    this.saveJson();
                } catch (IOException e) { e.printStackTrace(); }
                syncConflict.set(false);
                Settings.webSocketAPI.sendMessage(new CloseModalData());
            })));
            while (syncConflict.get())
            {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) { e.printStackTrace(); }
            }
        }
        else
        {
            cloudSyncLoop(this.path, false, CloudSyncType.SYNC_NORMAL, s3ObjectSummaries);
            Settings.webSocketAPI.sendMessage(new CloseModalData());
        }
        CreeperLauncher.isSyncing.set(false);
    }

    public void cloudSyncLoop(Path path, boolean ignoreInUse, CloudSyncType cloudSyncType, HashMap<String, S3ObjectSummary> existingObjects)
    {
        final String host = Constants.S3_HOST;
        final int port = 8080;
        final String accessKeyId = Constants.S3_KEY;
        final String secretAccessKey = Constants.S3_SECRET;
        final String bucketName = Constants.S3_BUCKET;

        Path baseInstancesPath = Settings.getInstanceLocOr(Constants.INSTANCES_FOLDER_LOC);

        CloudSaveManager.setup(host, port, accessKeyId, secretAccessKey, bucketName);
        if(Files.isDirectory(path))
        {
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
        }
        else
        {
            try
            {
                LOGGER.debug("Uploading file {}", path.toAbsolutePath());
                switch (cloudSyncType)
                {
                    case SYNC_NORMAL:
                        try
                        {
                            ArrayList<CompletableFuture<?>> futures = new ArrayList<>();
                            futures.add(CompletableFuture.runAsync(() ->
                            {
                                try
                                {
                                    CloudSaveManager.syncFile(path, CloudSaveManager.fileToLocation(path, baseInstancesPath), true, existingObjects);
                                } catch (Exception e) { e.printStackTrace(); }
                            }, DownloadTask.threadPool));

                            allFutures(futures).join();
                        } catch (Throwable t)
                        {
                            LOGGER.error(t);
                        }
                        break;
                    case SYNC_MANUAL_CLIENT:
                        CloudSaveManager.syncManual(path, CloudSaveManager.fileToLocation(path, Settings.getInstanceLocOr(Constants.INSTANCES_FOLDER_LOC)), true, true, existingObjects);
                        break;
                    case SYNC_MANUAL_SERVER:
                        CloudSaveManager.syncManual(path, CloudSaveManager.fileToLocation(path, Settings.getInstanceLocOr(Constants.INSTANCES_FOLDER_LOC)), true, false, existingObjects);
                        break;
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    public List<ModFile> getMods() {
        try {
            return Files.walk(path.resolve("mods")).filter(Files::isRegularFile).filter(file -> ModFile.isPotentialMod(file.toString())).map(path -> {
                File file = path.toFile();
                return new ModFile(file.getName(), "", file.length(), "").setPath(path);
            }).collect(Collectors.toList());
        } catch (IOException error) {
            LOGGER.log(Level.DEBUG, "Error occurred whilst listing mods on disk", error);
        }

        return new ArrayList<>();
    }
}
