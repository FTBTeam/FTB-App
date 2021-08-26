package net.creeperhost.creeperlauncher;

import com.google.gson.JsonObject;
import com.install4j.api.launcher.ApplicationLauncher;
import com.install4j.api.update.UpdateChecker;
import net.covers1624.quack.logging.log4j2.Log4jUtils;
import net.creeperhost.creeperlauncher.api.WebSocketAPI;
import net.creeperhost.creeperlauncher.api.data.other.*;
import net.creeperhost.creeperlauncher.install.tasks.FTBModPackInstallerTask;
import net.creeperhost.creeperlauncher.install.tasks.LocalCache;
import net.creeperhost.creeperlauncher.migration.MigrationManager;
import net.creeperhost.minetogether.lib.vpn.MineTogetherConnect;
import net.creeperhost.creeperlauncher.os.OS;
import net.creeperhost.creeperlauncher.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class CreeperLauncher
{
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Object DIE_LOCK = new Object();

    public static HashMap<String, String> javaVersions;
    public static int missedPings = 0;
    public static ServerSocket serverSocket = null;
    public static Socket socket = null;
    public static OutputStream socketWrite = null;
    public static boolean opened = false;
    public static Executor taskExeggutor = Executors.newWorkStealingPool();

    static
    {
        Log4jUtils.redirectStreams();
        System.setProperty("apple.awt.UIElement", "true");
    }

    public static Process elect = null;
    public static boolean isDevMode = false;
    public static AtomicBoolean isInstalling = new AtomicBoolean(false);
    public static AtomicReference<FTBModPackInstallerTask> currentInstall = new AtomicReference<>();

    public static LocalCache localCache;

    public static boolean defaultWebsocketPort = false;
    public static int websocketPort = WebSocketAPI.generateRandomPort();
    public static final String websocketSecret = WebSocketAPI.generateSecret();
    public static boolean websocketDisconnect=false;
    public static AtomicBoolean isSyncing = new AtomicBoolean(false);
    public static AtomicReference<List<Process>> mojangProcesses = new AtomicReference<>();
    public static MineTogetherConnect mtConnect;

    private static boolean warnedDevelop = false;

    public static boolean verbose = false;

    public CreeperLauncher() {}

    public static void initSettingsAndCache() {
        Settings.loadSettings();
        localCache = new LocalCache(Settings.getInstanceLocOr(Constants.INSTANCES_FOLDER_LOC).resolve(".localCache"));
    }

    public static void main(String[] args)
    {
        // Do this as soon as possible. Settings and instance location SHOULD be right by this point
        // (with a slim chance of legacy migration), but better to have some settings than none
        Settings.loadSettings();
        Instances.refreshInstances();


        /*
        Borrowed from ModpackServerDownloader project - parse args
         */
        HashMap<String, String> Args = new HashMap<>();
        String argName = null;
        for(String arg : args)
        {
            if(arg.length() > 2) {
                if (arg.startsWith("--")) {
                    argName = arg.substring(2);
                    Args.put(argName, "");
                }
                if (argName != null) {
                    if (argName.length() > 2) {
                        if (!argName.equals(arg.substring(2))) {
                            if (Args.containsKey(argName)) {
                                Args.remove(argName);
                            }
                            Args.put(argName, arg);
                            argName = null;
                        }
                    }
                }
            }
        }
        /*
        End
         */

        boolean isOverwolf = Args.containsKey("overwolf");
        boolean startProcess = !isDevMode;

        if(isDevMode || isOverwolf){
            startProcess = false;
            defaultWebsocketPort = true;
        }

        isDevMode = Args.containsKey("dev");

        if(isOverwolf)
        {
            LOGGER.info("Overwolf integration mode");
        }
        if(Args.containsKey("pid") && !isDevMode)
        {
            try {
                long pid = Long.parseLong(Args.get("pid"));
                Optional<ProcessHandle> electronProc = ProcessHandle.of(pid);
                if (electronProc.isPresent())
                {
                    startProcess = false;
                    defaultWebsocketPort = true;
                    ProcessHandle handle = electronProc.get();
                    handle.onExit().thenRun(() ->
                    {
                        while (isSyncing.get()) {
                            try {
                                Thread.sleep(1000); // TODO: Replace with a a wait/notify
                            } catch (InterruptedException e) { e.printStackTrace(); }
                        }
                        CreeperLauncher.exit();
                    });
                    if(!isOverwolf) Runtime.getRuntime().addShutdownHook(new Thread(handle::destroy));
                }
            } catch (Exception exception) {
                LOGGER.error("Error connecting to process", exception);
            }
        } else {
            if(isDevMode)
            {
                LOGGER.info("Development mode");
            } else {
                LOGGER.info("No PID args");
            }
        }


        try {
            Settings.webSocketAPI = new WebSocketAPI(new InetSocketAddress(InetAddress.getLoopbackAddress(), defaultWebsocketPort || isDevMode ? Constants.WEBSOCKET_PORT : websocketPort));
            Settings.webSocketAPI.setConnectionLostTimeout(0);
            Settings.webSocketAPI.start();
            if(OS.CURRENT == OS.WIN) pingPong();
        } catch(Throwable t)
        {
            websocketDisconnect=true;
            LOGGER.error("Unable to open websocket port or websocket has disconnected...", t);
        }

        if (startProcess) {
            startElectron();
        }

        MigrationManager migrationManager = new MigrationManager();
        migrationManager.doMigrations();

        // Reload in case settings changed. Ideally we want the front end to wait until the back end says "Ok we ready
        // bois" before the front end requests any information but that's a further issue, not for this release
        initSettingsAndCache();
        Instances.refreshInstances();

        doUpdate(args);


        FileUtils.listDir(Constants.WORKING_DIR).stream()
                .filter(e -> e.getFileName().toString().endsWith(".jar") && !e.getFileName().toString().contains(Constants.APPVERSION))
                .forEach(e -> {
                    try {
                        Files.deleteIfExists(e);
                    } catch (IOException ignored) {
                    }
                });

        registerSettingsListeners(args);

        CompletableFuture.runAsync(localCache::clean);

        if(!Files.isWritable(Constants.getDataDir()))
        {
            OpenModalData.openModal("Critical Error", "The FTBApp is unable to write to your selected data directory, this can be caused by file permission errors, anti-virus or any number of other configuration issues.<br />If you continue, the app will not work as intended and you may be unable to install or run any modpacks.", List.of(
                    new OpenModalData.ModalButton( "Exit", "green", CreeperLauncher::exit),
                    new OpenModalData.ModalButton("Continue", "", () -> {
                        Settings.webSocketAPI.sendMessage(new CloseModalData());
                    }))
            );
        }

        MiscUtils.updateJavaVersions();

        //Hang indefinitely until this lock is interrupted.
        try {
            synchronized (DIE_LOCK) {
                DIE_LOCK.wait();
            }
        } catch (InterruptedException ignored) {
        }
    }

    private static void registerSettingsListeners(String[] args)
    {
        SettingsChangeUtil.registerListener("instanceLocation", (key, value) -> {
            OpenModalData.openModal("Confirmation", "Are you sure you wish to move your instances to this location? <br tag='haha line break go brr'> All content in your current instance location will be moved, and if content exists with the same name in the destination it will be replaced.", List.of(
                    new OpenModalData.ModalButton( "Yes", "green", () -> {
                        OpenModalData.openModal("Please wait", "Your instances are now moving", List.of());
                        Path currentInstanceLoc = Path.of(Settings.settings.getOrDefault(key, Constants.INSTANCES_FOLDER_LOC.toAbsolutePath().toString()));
                        List<Path> subFiles = FileUtils.listDir(currentInstanceLoc);

                        if(value == null || value.isEmpty())
                        {
                            OpenModalData.openModal("Failed", "Instance Location can not be blank", List.of(
                                    new OpenModalData.ModalButton( "OK", "green", () -> Settings.webSocketAPI.sendMessage(new CloseModalData()))
                            ));
                            return;
                        }

                        Path newInstanceDir = Path.of(value);
                        boolean failed = false;
                        HashMap<Pair<Path, Path>, IOException> lastError = new HashMap<>();
                        LOGGER.info("Moving instances from {} to {}", currentInstanceLoc, value);
                        if (subFiles != null) {
                            for (Path file : subFiles) {
                                String fileName = file.getFileName().toString();
                                if(fileName.length() == 36) {
                                    try {
                                        //noinspection ResultOfMethodCallIgnored
                                        UUID.fromString(fileName);
                                    } catch (Throwable ignored) {
                                        continue;
                                    }
                                } else if (!fileName.equals(".localCache")) {
                                    continue;
                                }
                                Path dstPath = newInstanceDir.resolve(fileName);
                                lastError = FileUtils.move(file, dstPath, true, true);
                                failed = !lastError.isEmpty() && !fileName.equals(".localCache");
                                if (failed) break;
                                LOGGER.info("Moved {} to {} successfully", file, dstPath);
                            }
                        }
                        if (failed) {
                            LOGGER.error("Error occurred whilst migrating instances to the new location. Errors follow.");
                            lastError.forEach((moveKey, moveValue) -> {
                                LOGGER.error("Moving {} to {} failed:", moveKey.getLeft(), moveKey.getRight(), moveValue);
                            });
                            LOGGER.error("Moving any successful instance moves back");
                            List<Path> newInstanceDirFiles = FileUtils.listDir(newInstanceDir);
                            if (newInstanceDirFiles != null) {
                                for (Path file : newInstanceDirFiles) {
                                    FileUtils.move(file, currentInstanceLoc.resolve(file.getFileName()));
                                }
                            }
                            OpenModalData.openModal("Error", "Unable to move instances. Please ensure you have permission to create files and folders in this location.", List.of(
                                    new OpenModalData.ModalButton("Ok", "red", () -> Settings.webSocketAPI.sendMessage(new CloseModalData()))
                            ));
                        } else {
                            Path oldCache = Settings.getInstanceLocOr(Constants.INSTANCES_FOLDER_LOC).resolve(".localCache");
                            oldCache.toFile().deleteOnExit();
                            Settings.settings.remove("instanceLocation");
                            Settings.settings.put("instanceLocation", value);
                            Settings.saveSettings();
                            Instances.refreshInstances();
                            localCache = new LocalCache(Settings.getInstanceLocOr(Constants.INSTANCES_FOLDER_LOC).resolve(".localCache"));
                            OpenModalData.openModal("Success", "Moved instance folder location successfully", List.of(
                                    new OpenModalData.ModalButton( "Yay!", "green", () -> Settings.webSocketAPI.sendMessage(new CloseModalData()))
                            ));
                        }
                    }),
                    new OpenModalData.ModalButton("No", "red", () -> Settings.webSocketAPI.sendMessage(new CloseModalData()))
            ));
            return false;
        });


        SettingsChangeUtil.registerListener("enablePreview", (key, value) -> {
            if (Settings.settings.getOrDefault("enablePreview", "").isEmpty() && value.equals("false")) return true;
            if (Constants.BRANCH.equals("release") || Constants.BRANCH.equals("preview"))
            {
                OpenModalData.openModal("Update", "Do you wish to change to this branch now?", List.of(
                        new OpenModalData.ModalButton( "Yes", "green", () -> {
                            doUpdate(args);
                        }),
                        new OpenModalData.ModalButton( "No", "red", () -> {
                            Settings.webSocketAPI.sendMessage(new CloseModalData());
                        })
                ));
                return true;
            } else {
                if (!warnedDevelop)
                {
                    warnedDevelop = true;
                    OpenModalData.openModal("Update", "Unable to switch from branch " + Constants.BRANCH + " via this toggle.", List.of(
                            new OpenModalData.ModalButton("Ok", "red", () -> Settings.webSocketAPI.sendMessage(new CloseModalData()))
                    ));
                }
                return false;
            }
        });

        SettingsChangeUtil.registerListener("verbose", (key, value) -> {
            verbose = value.equals("true");
            return true;
        });
    }

    @SuppressWarnings("ConstantConditions")
    private static void doUpdate(String[] args) {
        String preview = Settings.settings.getOrDefault("enablePreview", "");
        String[] updaterArgs = new String[]{};
        if (Constants.BRANCH.equals("release") && preview.equals("true"))
        {
            updaterArgs = new String[] {"-VupdatesUrl=https://apps.modpacks.ch/FTBApp/preview.xml", "-VforceUpdate=true"};
        } else if (Constants.BRANCH.equals("preview") && !preview.isEmpty() && !preview.equals("true")) {
            updaterArgs = new String[] {"-VupdatesUrl=https://apps.modpacks.ch/FTBApp/release.xml", "-VforceUpdate=true"};
        }
        //Auto update - will block, kill us and relaunch if necessary
        try
        {
            ApplicationLauncher.launchApplicationInProcess("346", updaterArgs, null, null, null);

            if (UpdateChecker.isUpdateScheduled())
            {
                UpdateChecker.executeScheduledUpdate(Arrays.asList("-q", "-splash", "\"Updating...\""), true, Arrays.asList(args), null);
            }
        } catch (Throwable ignored)
        {
        }
    }
    public static long unixtimestamp()
    {
        return System.currentTimeMillis() / 1000L;
    }
    private static void pingPong()
    {
        CompletableFuture.runAsync(() -> {
            while(true)
            {
                try {
                    PingLauncherData ping = new PingLauncherData();
                    CreeperLauncher.missedPings++;
                    Settings.webSocketAPI.sendMessage(ping);
                } catch(Exception ignored) {}
                try {
                    Thread.sleep(3000);
                } catch(Exception ignored) {}
                //15 minutes without ping/pong or an explicit disconnect event happened...
                if(missedPings > 300 || websocketDisconnect && missedPings > 3)
                {
                    break;
                }
            }
        }).thenRun(() -> {
            if (!websocketDisconnect) {
                LOGGER.error("Closed backend due to no response from frontend for {} seconds...", (missedPings * 3));
            } else {
                LOGGER.error("Closed backend due to websocket error! Also no messages from frontend for {} seconds.", (missedPings * 3));
            }
            CreeperLauncher.exit();
        });
    }
    public static void listenForClient(int port) throws IOException {
        LOGGER.info("Starting mod socket on port {}", port);
        serverSocket = new ServerSocket(port);
        opened = true;
        socket = serverSocket.accept();
        socketWrite = socket.getOutputStream();
        LOGGER.info("Connection received");
        Runtime.getRuntime().addShutdownHook(new Thread(CreeperLauncher::closeOldClient));
        String lastInstance = "";
        ClientLaunchData.Reply reply;
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            long lastMessageTime = 0;
            boolean hasStarted = false;
            while (socket.isConnected()) {
                String bufferText = "";
                bufferText = in.readLine();
                if (bufferText.length() == 0) continue;
                JsonObject object = GsonUtils.GSON.fromJson(bufferText, JsonObject.class);
                Object data = new Object();
                if(!hasStarted) hasStarted = (object.has("message") && object.get("message").getAsString().equals("init"));
                if(hasStarted) {
                    if (object.has("data") && object.get("data") != null) {
                        data = object.get("data");
                    }
                    if (object.has("instance") && object.get("instance").getAsString() != null && object.get("instance").getAsString().length() > 0) {
                        lastInstance = object.get("instance").getAsString();
                    }
                    boolean isDone = (object.has("message") && object.get("message").getAsString().equals("done"));
                    if (System.currentTimeMillis() > (lastMessageTime + 200) || isDone) {
                        String type = (object.has("type") && object.get("type").getAsString() != null) ? object.get("type").getAsString() : "";
                        String message = (object.has("message") && object.get("message").getAsString() != null) ? object.get("message").getAsString() : "";
                        reply = new ClientLaunchData.Reply(lastInstance, type, message, data);
                        lastMessageTime = System.currentTimeMillis();
                        try {
                            Settings.webSocketAPI.sendMessage(reply);
                        } catch(Throwable t)
                        {
                            LOGGER.warn("Unable to send MC client loading update to frontend!", t);
                        }
                    }
                    if (isDone) {
                        closeSockets();
                    }
                }
            }
            closeSockets();
        } catch (Throwable e) {
            if(lastInstance.length() > 0) {
                reply = new ClientLaunchData.Reply(lastInstance, "clientDisconnect", new Object());
                Settings.webSocketAPI.sendMessage(reply);
            }

            closeSockets();

            throw e;
        } finally {
            if (in != null) in.close();
        }
    }

    private static void closeSockets() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }

        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {
        }

        try {
            if (socketWrite != null) socketWrite.close();
        } catch (IOException ignored) {
        }

        socket = null;
        serverSocket = null;
        socketWrite = null;
    }

    public static void closeOldClient() {
        if(socket != null && socket.isConnected()) {
            try {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("message", "show");
                socket.getOutputStream().write((jsonObject.toString() + "\n").getBytes());
                closeSockets();
            } catch (IOException ignored) {
            }
        }
    }

    private static void startElectron() {
        Path electron;

        ArrayList<String> args = new ArrayList<>();


        switch (OS.CURRENT)
        {
            case MAC:
                electron = Constants.BIN_LOCATION_OURS.resolve("ftbapp.app");
                args.add(0, electron.resolve("Contents/MacOS/ftbapp").toAbsolutePath().toString());
                break;
            case LINUX:
                electron = Constants.BIN_LOCATION_OURS.resolve("ftb-app");
                FileUtils.setFilePermissions(electron);

                args.add(0, electron.toAbsolutePath().toString());

                try {
                    if (Files.exists(Path.of("/proc/sys/kernel/unprivileged_userns_clone")) && new String(Files.readAllBytes(Path.of("/proc/sys/kernel/unprivileged_userns_clone"))).equals("0"))
                    {
                        args.add(1,  "--no-sandbox");
                    }
                } catch (IOException ignored) {}
                break;
            default:
                electron = Constants.BIN_LOCATION_OURS.resolve("ftbapp.exe");
                args.add(0, electron.toAbsolutePath().toString());
        }

        args.add("--ws");
        args.add(websocketPort + ":" + websocketSecret);
        args.add("--pid");
        args.add(String.valueOf(ProcessHandle.current().pid()));

        ProcessBuilder app = new ProcessBuilder(args);

        if (Files.exists(electron))
        {
            try
            {
                LOGGER.info("Starting Electron: " + String.join(" ", args));
                elect = app.start();
                StreamGobblerLog.redirectToLogger(elect.getErrorStream(), LOGGER::error);
                StreamGobblerLog.redirectToLogger(elect.getInputStream(), LOGGER::info);
            } catch (IOException e)
            {
                LOGGER.error("Error starting Electron: ", e);
            }
            CompletableFuture<Process> completableFuture = elect.onExit();
            completableFuture.thenRun(CreeperLauncher::exit);
            Runtime.getRuntime().addShutdownHook(new Thread(elect::destroy));
        }
    }

    public static void exit() {
        try
        {
            Settings.webSocketAPI.stop();
            if(CreeperLauncher.mtConnect != null && CreeperLauncher.mtConnect.isEnabled() && CreeperLauncher.mtConnect.isConnected())
            {
                CreeperLauncher.mtConnect.disconnect();
            }
        } catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
        }
        Settings.saveSettings();
        System.exit(0);
    }
}

