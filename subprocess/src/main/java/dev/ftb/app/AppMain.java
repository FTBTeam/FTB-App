package dev.ftb.app;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dev.ftb.app.accounts.AccountManager;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.WebsocketServer;
import dev.ftb.app.api.data.other.CloseModalData;
import dev.ftb.app.api.data.other.OpenModalData;
import dev.ftb.app.api.data.other.PingLauncherData;
import dev.ftb.app.install.tasks.LocalCache;
import dev.ftb.app.migration.MigrationsManager;
import dev.ftb.app.os.OS;
import dev.ftb.app.storage.settings.Settings;
import dev.ftb.app.task.LongRunningTaskManager;
import dev.ftb.app.util.*;
import net.covers1624.jdkutils.JavaInstall;
import net.covers1624.jdkutils.locator.JavaLocator;
import net.covers1624.quack.logging.log4j2.Log4jUtils;
import net.covers1624.quack.platform.Architecture;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class AppMain {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Object DIE_LOCK = new Object();

    public static List<Pair<String, String>> javaVersions = List.of();
    public static Executor taskExecutor = Executors.newWorkStealingPool();
    
    public static int screenWidth = 1080;
    public static int screenHeight = 720;

    static {
        Log4jUtils.redirectStreams();
        SSLUtils.inject();
        System.setProperty("apple.awt.UIElement", "true");
    }
    
    // He a wide boi
    public static LongRunningTaskManager LONG_TASK_MANAGER = new LongRunningTaskManager();

    public static ExecutorService INSTANCE_LAUNCHER_POOL = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("Instance Launcher %d").setDaemon(true).build());

    public static LocalCache localCache;

    public static boolean websocketDisconnect = false;
    public static AtomicBoolean isSyncing = new AtomicBoolean(false);
    
    private static AppMain INSTANCE = null;
    
    public static void main(String[] args) {
        ImmutableMap<String, String> parsedArgs = ImmutableMap.of();
        try {
            parsedArgs = StartArgParser.parse(args).getArgs();
        } catch (Throwable ex) {
            LOGGER.error("Failed to parse args, continuing with empty args", ex);
        }
        
        var app = new AppMain(parsedArgs);
        INSTANCE = app;
        
        try {
            app.startApp();
        } catch (Throwable ex) {
            LOGGER.error("Failed to start app", ex);
        }
    }
    
    private final ImmutableMap<String, String> startArgs;
    
    private final AppFlags flags;
    private final AppPaths paths;
    private final AccountManager accounts;
    private final Settings settings;
    
    public AppMain(ImmutableMap<String, String> args) {
        this.startArgs = args;
        
        this.flags = new AppFlags(
            args.containsKey("dev"),
            args.containsKey("overwolf") ? AppFlags.FrontendPlatform.OVERWOLF : AppFlags.FrontendPlatform.ELECTRON
        );
        
        this.paths = new AppPaths();
        this.settings = new Settings(this.paths);
        this.accounts = new AccountManager(this.paths);
    }
    
    public static AppMain instance() {
        return INSTANCE;
    }

    public AccountManager accounts() {
        return accounts;
    }
    
    public AppPaths paths() {
        return paths;
    }

    public void initSettingsAndCache() {
        localCache = new LocalCache(this.settings.getInstancesDir().resolve(".localCache"));
    }

    public void startApp() {        
        Constants.PLATFORM = this.startArgs.containsKey("overwolf") ? "overwolf" : "electron";
        
        if (this.startArgs.containsKey("jetty-verbose")) {
            Configurator.setLevel("org.eclipse.jetty", Level.DEBUG);
            LOGGER.info("Jetty verbose logging enabled");
        }

        if (this.startArgs.containsKey("screenWidth") && this.startArgs.containsKey("screenHeight")) {
            try {
                screenWidth = Integer.parseInt(this.startArgs.getOrDefault("screenWidth", "1080"));
                screenHeight = Integer.parseInt(this.startArgs.getOrDefault("screenHeight", "720"));
            } catch (Exception ex) {
                LOGGER.error("Failed to parse screen dimensions, using defaults", ex);
            }
        }
        
        this.logAppDetails();

        try {
            this.prelaunchChecks();
        } catch (Throwable ex) {
            LOGGER.error("Prelaunch checks failed but are not fatal", ex);
        }
        
        // If this fails, we're fucked.
        this.mainImpl();
    }
    
    private void mainImpl() {        
        // Cleanup before shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(this::cleanUpBeforeExit));
        
        Instances.refreshInstances();

        LOGGER.info("{} integration mode", this.flags.isOverwolf() ? "Overwolf" : "Electron");

        // Hook the pid so we can shutdown the frontend when it's closed
        if (startArgs.containsKey("pid") && !this.flags.isDev()) {
            try {
                long pid = Long.parseLong(startArgs.get("pid"));
                Optional<ProcessHandle> frontendProcess = ProcessHandle.of(pid);
                if (frontendProcess.isPresent()) {
                    ProcessHandle handle = frontendProcess.get();
                    handle.onExit().thenRun(() -> {
                        while (isSyncing.get()) {
                            try {
                                Thread.sleep(1000); // TODO: Replace with a a wait/notify
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        this.exit();
                    });
                    if (!this.flags.isOverwolf()) Runtime.getRuntime().addShutdownHook(new Thread(handle::destroy));
                }
            } catch (Exception exception) {
                LOGGER.error("Error connecting to process", exception);
            }
        } else {
            LOGGER.info(this.flags.isDev() ? "Development mode" : "No PID args specified");
        }
        
        ModpackApiUtils.loadPrivateToken();

        try {
            WebsocketServer.PortMode portMode;
            if (this.flags.isDev()) {
                portMode = WebsocketServer.PortMode.STATIC;
            } else {
                portMode = WebsocketServer.PortMode.DYNAMIC;
            }
            
            var port = WebSocketHandler.startWebsocket(portMode);
            // Common format both electron and overwolf can understand
            LOGGER.info("Backend Ready! Port=%s OneTimeToken=%s".formatted(port, Constants.WEBSOCKET_SECRET), port);
            if (OS.CURRENT == OS.WIN) pingPong();
        } catch (Throwable t) {
            websocketDisconnect = true;
            LOGGER.error("Unable to open websocket port or websocket has disconnected...", t);
        }
        
        // Reload in case settings changed. Ideally we want the front end to wait until the back end says "Ok we ready
        // bois" before the front end requests any information but that's a further issue, not for this release
        this.initSettingsAndCache();

        FileUtils.listDir(paths().workingDir()).stream()
            .filter(e -> e.getFileName().toString().endsWith(".jar") && !e.getFileName().toString().contains(Constants.APPVERSION))
            .forEach(e -> {
                try {
                    Files.deleteIfExists(e);
                } catch (IOException ignored) {
                    LOGGER.error("Failed to remove {} from {}", e.toString(), paths().workingDir());
                }
            });

        registerSettingsListeners();

        CompletableFuture.runAsync(localCache::clean);

        if (!Files.isWritable(this.paths().workingDir())) {
            OpenModalData.openModal("Critical Error", "The FTBApp is unable to write to your selected data directory, this can be caused by file permission errors, anti-virus or any number of other configuration issues.<br />If you continue, the app will not work as intended and you may be unable to install or run any modpacks.", List.of(
                new OpenModalData.ModalButton("Exit", "success", this::exit),
                new OpenModalData.ModalButton("Continue", "danger", () -> {
                    WebSocketHandler.sendMessage(new CloseModalData());
                }))
            );
        }

        updateJavaVersions();
        
        // Lastly, run any needed migrations after the app is ready
        MigrationsManager.INSTANCE.runMigrations();

        //Hang indefinitely until this lock is interrupted.
        try {
            synchronized (DIE_LOCK) {
                DIE_LOCK.wait();
            }
        } catch (InterruptedException ignored) {
        }
    }

    public void updateJavaVersions() {
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

    private void registerSettingsListeners() {
        SettingsChangeUtil.addChangeListener(oldSettings -> ProxyUtils.loadProxy());
    }

    private void pingPong() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            try {
                WebSocketHandler.sendMessage(new PingLauncherData());
            } catch (Exception ignored) {
                LOGGER.warn("Failed to send ping");
            }
        }, 0, 3, TimeUnit.SECONDS);
    }

    public void cleanUpBeforeExit() {
        LOGGER.info("Cleaning up for shutdown");
        WebSocketHandler.stopWebsocket();
        this.settings.save();
    }

    public void exit() {
        cleanUpBeforeExit();
        System.exit(0);
    }

    /**
     * Only allow the app to run one instance at a time. Otherwise things get messy!
     */
    private void prelaunchChecks() {
        if (this.startArgs.containsKey("ignore-pid-checks")) {
            LOGGER.info("Ignoring PID checks as requested");
            return;
        }

        // Read the pid and check if the process is still running
        if (Files.exists(paths().processMarkerFile())) {
            try {
                var data = Files.readString(paths().processMarkerFile());
                var pid = Long.parseLong(data.trim());
                ProcessHandle.of(pid).ifPresent((handle) -> {
                    if (!isOwnExecutable(handle)) {
                        LOGGER.warn("Pid {} from app.pid file does not appear to belong to a previous instance of this app (likely reused by an unrelated process). Not terminating it.", pid);
                        return;
                    }
                    // SHUT IT DOWN!
                    LOGGER.info("Found running process with pid {}", pid);
                    handle.destroy();
                });
            } catch (Throwable ex) {
                LOGGER.warn("Failed to read app.pid file", ex);
            }
        }

        // Write the current pid to the file
        var currentPid = ProcessHandle.current().pid();
        try {
            Files.writeString(paths().processMarkerFile(), String.valueOf(currentPid));
        } catch (IOException ex) {
            LOGGER.error("Failed to write app.pid file", ex);
        }
    }

    /**
     * Checks that a process handle belongs to the same executable as this app. This is to avoid killing unrelated processes that happen to have the same pid.
     */
    private boolean isOwnExecutable(ProcessHandle handle) {
        Optional<String> ourCommand = ProcessHandle.current().info().command();
        Optional<String> theirCommand = handle.info().command();
        if (ourCommand.isEmpty() || theirCommand.isEmpty()) {
            return false;
        }

        return ourCommand.get().equals(theirCommand.get());
    }
    
    private void logAppDetails() {
        List<String> jvmArgs = new ArrayList<>();
        try {
            jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        } catch (Throwable ex) {
            LOGGER.error("Failed to get JVM args", ex);
        }

        LOGGER.debug("""
FTB App Subprocess startup;
- App
\t- Version:        {}
\t- Branch:         {}
\t- Platform:       {}
- System
\t- OS:             {}
\t- OS Version:     {}
\t- OS Arch:        {}
- Paths
\t- CWD:            {}
- Arguments
\t- Args:           {}
\t- JVM Args:       {}
        """,
            Constants.APPVERSION,
            Constants.BRANCH,
            Constants.PLATFORM,
            System.getProperty("os.name"),
            System.getProperty("os.version"),
            Architecture.current().name(),
            paths().workingDir().toAbsolutePath(),
            String.join(" ", startArgs.values()),
            jvmArgs
        );
    }
}

