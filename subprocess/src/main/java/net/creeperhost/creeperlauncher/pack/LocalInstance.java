package net.creeperhost.creeperlauncher.pack;

import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.gson.*;
import net.creeperhost.creeperlauncher.api.data.other.CloseModalData;
import net.creeperhost.creeperlauncher.api.data.other.OpenModalData;
import net.creeperhost.creeperlauncher.api.handlers.ModFile;
import net.creeperhost.creeperlauncher.minecraft.modloader.forge.ForgeJarModLoader;
import net.creeperhost.minetogether.lib.cloudsaves.CloudSaveManager;
import net.creeperhost.minetogether.lib.cloudsaves.CloudSyncType;
import net.creeperhost.creeperlauncher.install.tasks.DownloadTask;
import net.creeperhost.creeperlauncher.os.OS;
import net.creeperhost.creeperlauncher.os.Platform;
import net.creeperhost.creeperlauncher.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;

import net.creeperhost.creeperlauncher.*;
import net.creeperhost.creeperlauncher.api.DownloadableFile;
import net.creeperhost.creeperlauncher.install.tasks.FTBModPackInstallerTask;
import net.creeperhost.creeperlauncher.minecraft.GameLauncher;
import net.creeperhost.creeperlauncher.minecraft.McUtils;
import net.creeperhost.creeperlauncher.minecraft.Profile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.BindException;
import java.net.MalformedURLException;
import java.nio.channels.FileLock;
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
    private Path path;
    private long versionId;
    public String name;
    private int minMemory = 2048;
    private int recMemory = 4096;
    public int memory = Integer.parseInt(Settings.settings.getOrDefault("memory", "2048"));
    private String version;
    private Path dir;
    private List<String> authors;
    private String description;
    public String mcVersion;
    public String jvmArgs = Settings.settings.getOrDefault("jvmArgs", "");
    public boolean embeddedJre = Boolean.parseBoolean(Settings.settings.getOrDefault("embeddedjre", "true"));
    public Path jrePath = Settings.getPathOpt("jrepath", null);
    private String url;
    private String artUrl;
    public int width = Integer.parseInt(Settings.settings.getOrDefault("width", String.valueOf((int) Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 2)));
    public int height = Integer.parseInt(Settings.settings.getOrDefault("height", String.valueOf((int) Toolkit.getDefaultToolkit().getScreenSize().getHeight() / 2)));
    public String modLoader = "";
    private long lastPlayed;
    private boolean isModified = false;
    private boolean isImport = false;
    public boolean cloudSaves = false;
    public boolean hasInstMods = false;
    public boolean installComplete = true;
    public byte packType;

    private transient CompletableFuture launcherWait;
    private transient HashMap<String, instanceEvent> postInstall = new HashMap<>();
    private transient Runnable prePlay;
    private transient int loadingModPort;
    private transient boolean prePlayAsync;
    public transient boolean hasLoadingMod;
    private transient Runnable preUninstall;
    private transient boolean preUninstallAsync;
    private transient AtomicBoolean inUse = new AtomicBoolean(false);
    private transient HashMap<String, instanceEvent> gameCloseEvents = new HashMap<>();
    public transient ModPack manifest;
    private transient boolean updateManifest = false;

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
        this.lastPlayed = CreeperLauncher.unixtimestamp();
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
        this.lastPlayed = CreeperLauncher.unixtimestamp();
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
            this.lastPlayed = jsonOutput.lastPlayed;
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
                if (this.postInstall != null && this.postInstall.size() > 0)
                {
                    ArrayList<CompletableFuture<?>> futures = new ArrayList<>();
                    for(Map.Entry<String, instanceEvent> event : this.postInstall.entrySet())
                    {
                        futures.add(event.getValue().Run());
                    }
                    allFutures(futures).thenRunAsync(() -> {
                        FTBModPackInstallerTask.currentStage = FTBModPackInstallerTask.Stage.FINISHED;
                        CreeperLauncher.isInstalling.set(false);
                    });
                } else {
                    FTBModPackInstallerTask.currentStage = FTBModPackInstallerTask.Stage.FINISHED;
                    CreeperLauncher.isInstalling.set(false);
                }
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
            if (this.postInstall != null && this.postInstall.size() > 0)
            {
                ArrayList<CompletableFuture<?>> futures = new ArrayList<>();
                for(Map.Entry<String, instanceEvent> event : this.postInstall.entrySet())
                {
                    futures.add(event.getValue().Run());
                }
                allFutures(futures).thenRunAsync(() -> {
                    FTBModPackInstallerTask.currentStage = FTBModPackInstallerTask.Stage.FINISHED;
                    CreeperLauncher.isInstalling.set(false);
                });
            } else {
                FTBModPackInstallerTask.currentStage = FTBModPackInstallerTask.Stage.FINISHED;
                CreeperLauncher.isInstalling.set(false);
            }
            try
            {
                this.saveJson();
            } catch (IOException e) { e.printStackTrace(); }
        });
        return update;
    }
    public Profile toProfile()
    {
        return toProfile("");
    }
    public Profile toProfile(String extraArgs)
    {
        String totalArgs = Settings.settings.getOrDefault("jvmargs", "") + " " + jvmArgs;
        if(totalArgs.length() > 0 && extraArgs.length() > 0) totalArgs = totalArgs.trim() + " " + extraArgs.trim();
        return new Profile(getUuid().toString(), getName(), getMcVersion(), modLoader, MiscUtils.getDateAndTime(), "custom", dir, art, totalArgs, memory, width, height);
    }
    public Process play()
    {
        return play("", true);
    }
    public Process play(String extraArgs, boolean loadInApp)
    {
        Platform platform = OS.CURRENT.getPlatform();
        {
            McUtils.killOldMinecraft();
        }

        if(hasInstMods)
        {
            ForgeJarModLoader.prePlay(this);
        }

        if (this.prePlay != null)
        {
            if (this.prePlayAsync)
            {
                LOGGER.debug("Doing pre-play tasks async");
                CompletableFuture.runAsync(this.prePlay);
            } else {
                LOGGER.debug("Doing pre-play tasks non async");
                this.prePlay.run();
            }
        }
        if(!Constants.S3_SECRET.isEmpty() && !Constants.S3_KEY.isEmpty() && !Constants.S3_HOST.isEmpty() && !Constants.S3_BUCKET.isEmpty()) {
            LOGGER.debug("Doing cloud sync");
            CompletableFuture.runAsync(() -> this.cloudSync(false)).join();
            onGameClose("cloudSync", () -> {
                if(cloudSaves) {
                    this.cloudSync(false);
                }
            });
        }
        McUtils.verifyJson(Constants.LAUNCHER_PROFILES_JSON);
        this.lastPlayed = CreeperLauncher.unixtimestamp();
        LOGGER.debug("Sending play request to API");
        Analytics.sendPlayRequest(this.getId(), this.getVersionId(), this.packType);
        LOGGER.debug("Clearing existing Mojang launcher profiles");
        McUtils.clearProfiles(Constants.LAUNCHER_PROFILES_JSON);
        long lastPlay = this.lastPlayed;
        this.lastPlayed = lastPlayed + 9001;
        LOGGER.debug("Injecting profile to Mojang launcher");


        this.hasLoadingMod = checkForLaunchMod();
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
            //END TESTING CODE
            if (this.hasLoadingMod) {
                CreeperLauncher.closeOldClient();
                int retries = 0;
                AtomicBoolean hasErrored = new AtomicBoolean(true);
                while (hasErrored.get()) {
                    //Retry ports...
                    hasErrored.set(false);
                    this.loadingModPort = MiscUtils.getRandomNumber(50001, 52000);
                    CompletableFuture.runAsync(() -> {
                        try {
                            CreeperLauncher.listenForClient(this.loadingModPort);
                        } catch(BindException err) {
                            LOGGER.error("Error whilst starting mod socket on port '{}'...", loadingModPort, err);
                            hasErrored.set(true);
                        } catch (Exception err) {
                            if (!CreeperLauncher.opened) {
                                LOGGER.warn("Error whilst handling message from mod socket - probably nothing!", err);
                                CreeperLauncher.opened = false;
                            }
                        }
                    });
                    try {
                        Thread.sleep(100);
                        if (retries >= 5) break;
                        retries++;
                    } catch (Exception ignored) {
                    }
                }
                if (!hasErrored.get()) {
                    if (extraArgs.length() > 0) extraArgs = extraArgs + " ";
                    extraArgs += "-Dchtray.port=" + this.loadingModPort + " -Dchtray.instance=" + this.uuid.toString() + " ";
                } else {
                    LOGGER.error("Unable to open loading mod listener port... Tried {} times.", retries);
                }
            }
        }

        Profile profile = (extraArgs.length() > 0) ? this.toProfile(extraArgs) : this.toProfile();
        if(jrePath != null) {
            if (jrePath.endsWith("javaw.exe") || jrePath.endsWith("java")) {
                if (Files.notExists(jrePath)) jrePath = null;
            } else {
                jrePath = null;
            }
        }
        if(!McUtils.injectProfile(Constants.BIN_LOCATION.resolve("launcher_profiles.json"), profile, jrePath))
        {
            LOGGER.error("Unable to inject Mojang launcher profile...");
            OpenModalData.openModal("Error", "Unable to create Mojang launcher profile. Please ensure you do not have any security software blocking access to the FTB App data directories.", List.of(
                    new OpenModalData.ModalButton("Ok", "red", () -> Settings.webSocketAPI.sendMessage(new CloseModalData()))
            ));
            return null;
        }
        LOGGER.info("Starting launcher at {}", Constants.BIN_LOCATION);

        this.lastPlayed = lastPlay;
        try {
            LOGGER.debug("Saving instance json");
            this.saveJson();
        } catch(Exception e) {
            LOGGER.error("Failed to save instance!", e);
        }

        LOGGER.debug("Starting Mojang launcher");
        Process launcher = platform.tryStartLauncher();

        if (launcher == null) {
            //Already logged, no point continuing.
            return null;
        }

        CreeperLauncher.mojangProcesses.getAndUpdate(_processes -> {
            //Copy as we must not modify the input as this can potentially be re-run. \o/ atomics!
            List<Process> processes = _processes == null ? new ArrayList<>() : new ArrayList<>(_processes);

            processes.add(launcher);

            return processes;
        });

        if (Settings.settings.getOrDefault("automateMojang", "true").equalsIgnoreCase("true")){
            GameLauncher.tryAutomation(launcher);
        }

        if (CreeperLauncher.mtConnect != null) {
            if (CreeperLauncher.mtConnect.isEnabled()) {
                LOGGER.info("MineTogether Connect is enabled... Connecting...");
                CreeperLauncher.mtConnect.connect();
                onGameClose("MTC-Disconnect", () -> {
                    if (CreeperLauncher.mtConnect.isConnected()) {
                        LOGGER.info("MineTogether Connect is enabled... Disconnecting...");
                        CreeperLauncher.mtConnect.disconnect();
                    }
                });
            } else {
                LOGGER.info("MineTogether Connect is not enabled...");
            }
        } else {
            LOGGER.error("Unable to initialize MineTogether Connect!");
        }
        if (launcherWait != null && (!launcherWait.isDone())) launcherWait.cancel(true);

        launcherWait = CompletableFuture.runAsync(() -> {
           inUseCheck(launcher);
        });

        return launcher;
    }

    public void setPostInstall(Runnable lambda, boolean async)
    {
        this.postInstall.put("postInstall", new instanceEvent(lambda, !async));
    }

    public void setPrePlay(Runnable hook, boolean async)
    {
        this.prePlay = hook;
        this.prePlayAsync = async;
    }

    public void setPreUninstall(Runnable hook, boolean async)
    {
        this.preUninstall = hook;
        this.preUninstallAsync = async;
    }

    public boolean uninstall() throws IOException
    {
        if (this.preUninstall != null)
        {
            if (this.preUninstallAsync)
            {
                CompletableFuture.runAsync(this.preUninstall);
            } else
            {
                this.preUninstall.run();
            }
        }
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
            writer.close();
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
    private transient CompletableFuture inUseThread;
    private void inUseCheck(Process vanillaLauncher)
    {
        if (inUseThread != null && !inUseThread.isDone()) return;
        inUseThread = CompletableFuture.runAsync(() -> {
            boolean fireEvents = false;
            while(true)
            {
                if(!vanillaLauncher.isAlive()) {
                    boolean inUse = isInUse(true);
                    if (!fireEvents) fireEvents = inUse;

                    if (fireEvents && !inUse) {

                        for (Map.Entry<String, instanceEvent> event : gameCloseEvents.entrySet()) {
                            LOGGER.info("Running game close event '{}'...", event.getKey());
                            event.getValue().Run();
                        }
                        fireEvents = false;
                    } else {
                        if (!fireEvents) {
                            break;
                        }
                    }
                } else {
                    if(!fireEvents) fireEvents = true;
                }
                try {
                    if(vanillaLauncher.isAlive()) {
                        Thread.sleep(250);
                    } else {
                        //Expensive file checking should happen less often...
                        Thread.sleep(5000);
                    }
                } catch (InterruptedException ignored) {}
            }
            LOGGER.debug("Game close event listener stopped...");
        });
    }
    public void onGameClose(String name, Runnable lambda)
    {
        if(gameCloseEvents.containsKey(name)) return;
        gameCloseEvents.put(name, new instanceEvent(lambda, name));
    }
    public boolean isInUse(boolean checkFiles)
    {
        if (inUse.get()) return true;
        if (checkFiles)
        {
            Path dir = getDir();
            if (Files.notExists(dir)) return false;
            Path modsFile = dir.resolve("mods");
            if (Files.exists(modsFile) && Files.isWritable(modsFile))
            {
                boolean fileLocked = false;
                try {
                    fileLocked = Files.walk(modsFile)
                            .filter(Files::isRegularFile)
                            .anyMatch(e -> {
                                try(FileLock ignored = new RandomAccessFile(e.toFile(), "rw").getChannel().tryLock()) {
                                    return false;
                                } catch (Throwable t) {
                                    return true;
                                }
                            });
                } catch (IOException ignored) {
                }
                if (fileLocked) {
                    return true;
                }
            }

            Path savesFile = dir.resolve("saves");
            if (Files.exists(savesFile) && Files.isDirectory(savesFile))
            {
                try {
                    return Files.walk(savesFile, 1)
                            .map(e -> e.resolve("session.lock"))
                            .anyMatch(Files::exists);
                } catch (IOException ignored) {
                }
            }
        }
        return false;
    }

    public void setInUse(boolean var)
    {
        inUse.set(var);
    }

    public void cloudSync(boolean forceCloud)
    {
        if(!cloudSaves || !Boolean.parseBoolean(Settings.settings.getOrDefault("cloudSaves", "false"))) return;
        OpenModalData.openModal("Please wait", "Checking cloud save synchronization <br>", List.of());

        if(isInUse(true)) return;

        AtomicInteger progress = new AtomicInteger(0);

        setInUse(true);
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
                            McUtils.killOldMinecraft().join();
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
        setInUse(false);
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
                        McUtils.killOldMinecraft();
                        CloudSaveManager.syncManual(path, CloudSaveManager.fileToLocation(path, Settings.getInstanceLocOr(Constants.INSTANCES_FOLDER_LOC)), true, true, existingObjects);
                        break;
                    case SYNC_MANUAL_SERVER:
                        McUtils.killOldMinecraft();
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
                return new ModFile(file.getName(), "", file.length(), "");
            }).collect(Collectors.toList());
        } catch (IOException error) {
            LOGGER.log(Level.DEBUG, "Error occurred whilst listing mods on disk", error);
        }

        return new ArrayList<>();
    }
}
