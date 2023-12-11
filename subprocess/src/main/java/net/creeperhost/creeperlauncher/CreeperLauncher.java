package net.creeperhost.creeperlauncher;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonObject;
import com.install4j.api.launcher.ApplicationLauncher;
import com.install4j.api.update.UpdateChecker;
import io.sentry.Sentry;
import io.sentry.log4j2.BuildConfig;
import io.sentry.protocol.SdkVersion;
import net.covers1624.jdkutils.JavaInstall;
import net.covers1624.jdkutils.JavaLocator;
import net.covers1624.quack.logging.log4j2.Log4jUtils;
import net.covers1624.quack.platform.Architecture;
import net.creeperhost.creeperlauncher.api.DebugTools;
import net.creeperhost.creeperlauncher.api.WebSocketAPI;
import net.creeperhost.creeperlauncher.api.data.other.ClientLaunchData;
import net.creeperhost.creeperlauncher.api.data.other.CloseModalData;
import net.creeperhost.creeperlauncher.api.data.other.OpenModalData;
import net.creeperhost.creeperlauncher.api.data.other.PingLauncherData;
import net.creeperhost.creeperlauncher.install.tasks.LocalCache;
import net.creeperhost.creeperlauncher.instance.cloud.CloudSaveManager;
import net.creeperhost.creeperlauncher.os.OS;
import net.creeperhost.creeperlauncher.task.LongRunningTaskManager;
import net.creeperhost.creeperlauncher.util.*;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class CreeperLauncher {

    // Yes, second static block, this will run before Log4j is initialized.
    // Must be here so Sentry doesn't re-configure itself twice and bork the appender.
    static {

        // noinspection ConstantConditions,MismatchedStringCase
        if (Constants.SENTRY_DSN.startsWith("https")) {
            Sentry.init(opts -> {
                opts.setDsn(Constants.SENTRY_DSN);

                // Yoinked from SentryAppender. There exists no way to configure attachServerName from system properties. We must init sentry manually.
                // Ideally I'd just want to set the sentry properties file and let log4j auto initialize it.
                opts.setSentryClientName(BuildConfig.SENTRY_LOG4J2_SDK_NAME);
                SdkVersion sdkVersion = SdkVersion.updateSdkVersion(opts.getSdkVersion(), BuildConfig.SENTRY_LOG4J2_SDK_NAME, BuildConfig.VERSION_NAME);
                sdkVersion.addPackage("maven:io.sentry:sentry-log4j2", BuildConfig.VERSION_NAME);
                //noinspection UnstableApiUsage
                opts.setSdkVersion(sdkVersion);
                // End Yoinked from SentryAppender.

                opts.setAttachServerName(false);
                opts.setEnableUncaughtExceptionHandler(true);
                opts.setRelease(Constants.APPVERSION);
                opts.setTag("branch", Constants.BRANCH);
                opts.setTag("platform", Constants.PLATFORM);
                opts.setTag("os.name", System.getProperty("os.name"));
                opts.setTag("os.version", System.getProperty("os.version"));
                opts.setTag("os.arch", Architecture.current().name());
            });
        }
    }

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Object DIE_LOCK = new Object();

    public static List<Pair<String, String>> javaVersions = List.of();
    public static int missedPings = 0;
    public static ServerSocket serverSocket = null;
    public static Socket socket = null;
    public static OutputStream socketWrite = null;
    public static boolean opened = false;
    public static Executor taskExeggutor = Executors.newWorkStealingPool();

    static {
        Log4jUtils.redirectStreams();
        SSLUtils.inject();
        DNSDebug.printDebugReport();
        System.setProperty("apple.awt.UIElement", "true");
    }

    public static Process elect = null;
    public static boolean isDevMode = false;

    // He a wide boi
    public static LongRunningTaskManager LONG_TASK_MANAGER = new LongRunningTaskManager();
    public static CloudSaveManager CLOUD_SAVE_MANAGER = new CloudSaveManager();

    public static ExecutorService INSTANCE_LAUNCHER_POOL = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("Instance Launcher %d").setDaemon(true).build());

    public static LocalCache localCache;

    public static boolean defaultWebsocketPort = false;
    public static int websocketPort = WebSocketAPI.generateRandomPort();
    public static final String websocketSecret = WebSocketAPI.generateSecret();
    public static boolean websocketDisconnect = false;
    public static AtomicBoolean isSyncing = new AtomicBoolean(false);

    private static boolean warnedDevelop = false;

    public static DebugTools DEBUG_TOOLS = DebugTools.NONE;

    public CreeperLauncher() {
    }

    public static void initSettingsAndCache() {
        Settings.loadSettings();
        localCache = new LocalCache(Settings.getInstancesDir().resolve(".localCache"));
    }

    public static void main(String[] args) {
        try {
            mainImpl(args);
        } catch (Throwable ex) {
            LOGGER.error("Main method threw exception:", ex);
        }
    }

    private static void mainImpl(String[] args) {
        // Cleanup before shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(CreeperLauncher::cleanUpBeforeExit));

        Settings.loadSettings();
        Instances.refreshInstances();

        ImmutableMap<String, String> Args = StartArgParser.parse(args).getArgs();

        isDevMode = Args.containsKey("dev");
        Constants.IS_DEV_MODE = isDevMode;

        boolean isOverwolf = Args.containsKey("overwolf");
        boolean startProcess = !isDevMode;

        if (isDevMode || isOverwolf) {
            startProcess = false;
            defaultWebsocketPort = true;
        }

        LOGGER.info((isOverwolf ? "Overwolf" : "Electron") + " integration mode");

        if (Args.containsKey("pid") && !isDevMode) {
            try {
                long pid = Long.parseLong(Args.get("pid"));
                Optional<ProcessHandle> electronProc = ProcessHandle.of(pid);
                if (electronProc.isPresent()) {
                    startProcess = false;
                    defaultWebsocketPort = true;
                    ProcessHandle handle = electronProc.get();
                    handle.onExit().thenRun(() ->
                    {
                        while (isSyncing.get()) {
                            try {
                                Thread.sleep(1000); // TODO: Replace with a a wait/notify
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        CreeperLauncher.exit();
                    });
                    if (!isOverwolf) Runtime.getRuntime().addShutdownHook(new Thread(handle::destroy));
                }
            } catch (Exception exception) {
                LOGGER.error("Error connecting to process", exception);
            }
        } else {
            LOGGER.info(isDevMode ? "Development mode" : "No PID args specified");
        }


        try {
            Settings.webSocketAPI = new WebSocketAPI(new InetSocketAddress(InetAddress.getLoopbackAddress(), defaultWebsocketPort || isDevMode ? Constants.WEBSOCKET_PORT : websocketPort));
            Settings.webSocketAPI.setConnectionLostTimeout(0);
            Settings.webSocketAPI.start();
            if (OS.CURRENT == OS.WIN) pingPong();
        } catch (Throwable t) {
            websocketDisconnect = true;
            LOGGER.error("Unable to open websocket port or websocket has disconnected...", t);
        }

        if (startProcess) {
            startElectron();
        }

        // Reload in case settings changed. Ideally we want the front end to wait until the back end says "Ok we ready
        // bois" before the front end requests any information but that's a further issue, not for this release
        initSettingsAndCache();

        doUpdate(args);

        FileUtils.listDir(Constants.WORKING_DIR).stream()
            .filter(e -> e.getFileName().toString().endsWith(".jar") && !e.getFileName().toString().contains(Constants.APPVERSION))
            .forEach(e -> {
                try {
                    Files.deleteIfExists(e);
                } catch (IOException ignored) {
                    LOGGER.error("Failed to remove {} from {}", e.toString(), Constants.WORKING_DIR);
                }
            });

        registerSettingsListeners(args);

        CompletableFuture.runAsync(localCache::clean);

        if (!Files.isWritable(Constants.getDataDir())) {
            OpenModalData.openModal("Critical Error", "The FTBApp is unable to write to your selected data directory, this can be caused by file permission errors, anti-virus or any number of other configuration issues.<br />If you continue, the app will not work as intended and you may be unable to install or run any modpacks.", List.of(
                new OpenModalData.ModalButton("Exit", "success", CreeperLauncher::exit),
                new OpenModalData.ModalButton("Continue", "danger", () -> {
                    Settings.webSocketAPI.sendMessage(new CloseModalData());
                }))
            );
        }

        updateJavaVersions();

        if (Boolean.getBoolean("Debugger.onStartup")) {
            openDebugTools();
        }

        //Hang indefinitely until this lock is interrupted.
        try {
            synchronized (DIE_LOCK) {
                DIE_LOCK.wait();
            }
        } catch (InterruptedException ignored) {
        }
    }

    public static void updateJavaVersions() {
        try {
            JavaLocator locator = JavaLocator.builder()
                .findIntellijJdks()
                .findGradleJdks()
                .useJavaw()
                .ignoreOpenJ9()
                .build();

            List<Pair<String, String>> entries = new LinkedList<>();
            entries.add(Pair.of("Recommended", ""));

            List<JavaInstall> installs = locator.findJavaVersions();
            installs.sort(Comparator.<JavaInstall, ComparableVersion>comparing(e -> new ComparableVersion(e.implVersion)).reversed());
            for (JavaInstall version : installs) {
                Path javaPath = JavaInstall.getJavaExecutable(version.javaHome, true).toAbsolutePath();
                String description = String.format("%s - %s %s (%s) - %s",
                    version.vendor,
                    version.implVersion,
                    version.hasCompiler ? "JDK" : "JRE",
                    version.architecture,
                    javaPath
                );
                entries.add(Pair.of(description, javaPath.toString()));
            }
            javaVersions = List.copyOf(entries);
        } catch (IOException ex) {
            LOGGER.error("Failed to poll available Java versions.", ex);
        }
    }

    private static void registerSettingsListeners(String[] args) {
        SettingsChangeUtil.registerChangeHandler("enablePreview", (key, value) -> {
            if (Settings.settings.getOrDefault("enablePreview", "").isEmpty() && value.equals("false")) return true;
            if (Constants.BRANCH.equals("release") || Constants.BRANCH.equals("preview")) {
                OpenModalData.openModal("Update", "Do you wish to change to this branch now?", List.of(
                    new OpenModalData.ModalButton("Yes", "success", () -> {
                        doUpdate(args);
                    }),
                    new OpenModalData.ModalButton("No", "danger", () -> {
                        Settings.webSocketAPI.sendMessage(new CloseModalData());
                    })
                ));
                return true;
            } else {
                if (!warnedDevelop) {
                    warnedDevelop = true;
                    OpenModalData.openModal("Update", "Unable to switch from branch " + Constants.BRANCH + " via this toggle.", List.of(
                        new OpenModalData.ModalButton("Ok", "danger", () -> Settings.webSocketAPI.sendMessage(new CloseModalData()))
                    ));
                }
                return false;
            }
        });

        SettingsChangeUtil.addChangeListener(oldSettings -> ProxyUtils.loadProxy());
    }

    @SuppressWarnings("ConstantConditions")
    private static void doUpdate(String[] args) {
        String preview = Settings.settings.getOrDefault("enablePreview", "");
        String[] updaterArgs = new String[]{};
        if (Constants.BRANCH.equals("release") && preview.equals("true")) {
            updaterArgs = new String[]{"-VupdatesUrl=https://apps.modpacks.ch/FTBApp/preview.xml", "-VforceUpdate=true"};
        } else if (Constants.BRANCH.equals("preview") && !preview.isEmpty() && !preview.equals("true")) {
            updaterArgs = new String[]{"-VupdatesUrl=https://apps.modpacks.ch/FTBApp/release.xml", "-VforceUpdate=true"};
        }
        //Auto update - will block, kill us and relaunch if necessary
        try {
            ApplicationLauncher.launchApplicationInProcess("346", updaterArgs, null, null, null);

            if (UpdateChecker.isUpdateScheduled()) {
                UpdateChecker.executeScheduledUpdate(Arrays.asList("-q", "-splash", "\"Updating...\""), true, Arrays.asList(args), null);
            }
        } catch (Throwable e) {

        }
    }

    private static void pingPong() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            try {
                PingLauncherData ping = new PingLauncherData();
                CreeperLauncher.missedPings++;
                Settings.webSocketAPI.sendMessage(ping);
            } catch (Exception ignored) {
                LOGGER.error("Failed to send ping");
            }

            // Don't do this in dev mode, when the frontend restarts, it can miss this amount of pings
            //15 minutes without ping/pong or an explicit disconnect event happened...
            if (!isDevMode && (missedPings > 300 || websocketDisconnect && missedPings > 3)) {
                if (!websocketDisconnect) {
                    LOGGER.error("Closed backend due to no response from frontend for {} seconds...", (missedPings * 3));
                } else {
                    LOGGER.error("Closed backend due to websocket error! Also no messages from frontend for {} seconds.", (missedPings * 3));
                }
                CreeperLauncher.exit();
            }
        }, 0, 3, TimeUnit.SECONDS);
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
                if (!hasStarted)
                    hasStarted = (object.has("message") && object.get("message").getAsString().equals("init"));
                if (hasStarted) {
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
                        } catch (Throwable t) {
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
            if (lastInstance.length() > 0) {
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
            LOGGER.error("Failed to close socket");
        }

        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {
            LOGGER.error("Failed to close server socket");
        }

        try {
            if (socketWrite != null) socketWrite.close();
        } catch (IOException ignored) {
            LOGGER.error("Failed to close socket writer");
        }

        socket = null;
        serverSocket = null;
        socketWrite = null;
    }

    public static void closeOldClient() {
        if (socket != null && socket.isConnected()) {
            try {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("message", "show");
                socket.getOutputStream().write((jsonObject.toString() + "\n").getBytes());
                closeSockets();
            } catch (IOException ignored) {
                LOGGER.error("Failed to close old client");
            }
        }
    }

    private static void startElectron() {
        Path electron;

        ArrayList<String> args = new ArrayList<>();


        switch (OS.CURRENT) {
            case MAC:
                electron = Constants.BIN_LOCATION_OURS.resolve("ftbapp.app");
                args.add(0, electron.resolve("Contents/MacOS/ftbapp").toAbsolutePath().toString());
                break;
            case LINUX:
                electron = Constants.BIN_LOCATION_OURS.resolve("ftb-app");
                FileUtils.setFilePermissions(electron);

                args.add(0, electron.toAbsolutePath().toString());

                try {
                    if (Files.exists(Path.of("/proc/sys/kernel/unprivileged_userns_clone")) && new String(Files.readAllBytes(Path.of("/proc/sys/kernel/unprivileged_userns_clone"))).equals("0")) {
                        args.add(1, "--no-sandbox");
                    }
                } catch (IOException ignored) {
                }
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

        if (Files.exists(electron)) {
            try {
                LOGGER.info("Starting Electron: " + String.join(" ", args));
                elect = app.start();
                StreamGobblerLog stdoutGobbler = new StreamGobblerLog()
                        .setName("Electron STDOUT gobbler")
                        .setInput(elect.getInputStream())
                        .setOutput(LOGGER::info);
                stdoutGobbler.start();
                StreamGobblerLog stderrGobbler = new StreamGobblerLog()
                        .setName("Electron STDERR gobbler")
                        .setInput(elect.getErrorStream())
                        .setOutput(LOGGER::warn);
                stderrGobbler.start();
                elect.onExit().thenRunAsync(() -> {
                    stdoutGobbler.stop();
                    stderrGobbler.stop();
                    CreeperLauncher.exit();
                });
            } catch (IOException e) {
                LOGGER.error("Error starting Electron: ", e);
            }
            Runtime.getRuntime().addShutdownHook(new Thread(elect::destroy));
        }
    }

    public static void cleanUpBeforeExit() {
        LOGGER.info("Cleaning up for shutdown");
        try {
            if (Settings.webSocketAPI != null) {
                Settings.webSocketAPI.stop();
            }

            closeSockets();
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Failed to close sockets...", e);
        }

        Settings.saveSettings();
    }

    public static void exit() {
        cleanUpBeforeExit();
        System.exit(0);
    }

    public static synchronized void openDebugTools() {
        if (!DebugTools.IS_AVAILABLE || DEBUG_TOOLS != DebugTools.NONE) return;
        LOGGER.info("Trying to open Debug Tools.");
        DEBUG_TOOLS = DebugTools.load();
    }

    public static void closeDebugTools() {
        if (DEBUG_TOOLS != DebugTools.NONE) {
            LOGGER.info("Closing Debug Tools.");
            DEBUG_TOOLS.close();
            DEBUG_TOOLS = DebugTools.NONE;
        }
    }
}

