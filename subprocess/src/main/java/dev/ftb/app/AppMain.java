package dev.ftb.app;

import dev.ftb.app.api.data.accounts.AccountsChangedEvent;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dev.ftb.app.api.WebsocketServer;
import dev.ftb.app.api.data.other.PingLauncherData;
import dev.ftb.app.migration.MigrationsManager;
import dev.ftb.app.os.OS;
import dev.ftb.app.services.AppServices;
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
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class AppMain {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Object DIE_LOCK = new Object();

    public static List<Pair<String, String>> javaVersions = List.of();
    
    public static int screenWidth = 1080;
    public static int screenHeight = 720;

    static {
        Log4jUtils.redirectStreams();
        SSLUtils.inject();
        System.setProperty("apple.awt.UIElement", "true");
    }
    
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
    
    // Built in startApp, once a previous instance has been stopped and the start args are applied
    private MigrationsManager migrations;
    private AppServices services;
    
    public AppMain(ImmutableMap<String, String> args) {
        this.startArgs = args;
        this.flags = new AppFlags(
            args.containsKey("dev"),
            args.containsKey("overwolf") ? AppFlags.FrontendPlatform.OVERWOLF : AppFlags.FrontendPlatform.ELECTRON
        );
        this.paths = new AppPaths();
    }
    
    public AppServices services() {
        return services;
    }

    public void startApp() {
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

        // Nothing works without the data folder, so stop before anything tries to write to it
        if (!Files.isWritable(this.paths.workingDir())) {
            this.failStartup(StartupFailure.DATA_DIR_NOT_WRITABLE);
            return;
        }

        try {
            this.prelaunchChecks();
        } catch (Throwable ex) {
            LOGGER.error("Prelaunch checks failed but are not fatal", ex);
        }
        
        // Services read and write settings, accounts and the vault, so they must not start until a previous
        // instance has been stopped. Settings defaults also need the screen size parsed above.
        this.services = new AppServices(this.flags, this.paths);
        this.migrations = new MigrationsManager(this.services);
        
        // If this fails, we're fucked.
        this.mainImpl();
    }
    
    private void mainImpl() {        
        // Cleanup before shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(this::cleanUpBeforeExit));

        // The Electron frontend restarts us if we go quiet for too long before "Backend Ready", so keep it informed
        // that we are still making progress during slow startups (lots of instances, slow disks, anti-virus).
        ScheduledExecutorService startupHeartbeat = startStartupHeartbeat();

        this.services.instances().refreshInstances();

        this.migrations.runMigrations();
        // The proxy was applied before migrations ran, so pick up a password that was just moved into the vault
        this.services().reloadProxy();

        var flags = this.services().flags();

        LOGGER.info("{} integration mode", flags.isOverwolf() ? "Overwolf" : "Electron");

        // Hook the pid so we can shutdown the frontend when it's closed
        if (startArgs.containsKey("pid") && !flags.isDev()) {
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
                    if (!flags.isOverwolf()) Runtime.getRuntime().addShutdownHook(new Thread(handle::destroy));
                }
            } catch (Exception exception) {
                LOGGER.error("Error connecting to process", exception);
            }
        } else {
            LOGGER.info(flags.isDev() ? "Development mode" : "No PID args specified");
        }
        
        // Keycloak ends offline sessions that go unused for too long, we should try and refresh them on start up
        CompletableFuture.runAsync(() -> this.services.accounts().ftb().refreshAll(), this.services.threads().taskPool())
            .thenRun(() -> this.services.rcpApi().sendMessage(new AccountsChangedEvent()))
            .exceptionally(e -> {
                LOGGER.error("Failed to refresh the FTB accounts at startup", e);
                return null;
            });

        try {
            WebsocketServer.PortMode portMode;
            if (flags.isDev()) {
                portMode = WebsocketServer.PortMode.STATIC;
            } else {
                portMode = WebsocketServer.PortMode.DYNAMIC;
            }
            
            var port = this.services().rcpApi().start(portMode);
            // Common format both electron and overwolf can understand. Written straight to stdout rather than logged
            System.out.println("Backend Ready! Port=%s OneTimeToken=%s".formatted(port, Constants.WEBSOCKET_SECRET));
            System.out.flush();
            LOGGER.info("RPC server started on port {}", port);
            if (OS.CURRENT == OS.WIN) pingPong();
        } catch (Throwable t) {
            websocketDisconnect = true;
            LOGGER.error("Unable to open websocket port or websocket has disconnected...", t);
        } finally {
            // Once the app is all started, we can shutdown the heartbeat single. 
            startupHeartbeat.shutdownNow();
        }

        // Reload in case settings changed. Ideally we want the front end to wait until the back end says "Ok we ready
        // bois" before the front end requests any information but that's a further issue, not for this release
//        this.initSettingsAndCache();

        FileUtils.listDir(this.services().paths().workingDir()).stream()
            .filter(e -> e.getFileName().toString().endsWith(".jar") && !e.getFileName().toString().contains(Constants.APPVERSION))
            .forEach(e -> {
                try {
                    Files.deleteIfExists(e);
                } catch (IOException ignored) {
                    LOGGER.error("Failed to remove {} from {}", e.toString(), this.services().paths().workingDir());
                }
            });

        CompletableFuture.runAsync(this.services.cache().local()::clean);

        updateJavaVersions();

        // Spins up threaded long-lived work for fixing instances.
        this.services.instances().scheduleMurmurFixing();

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

    private static ScheduledExecutorService startStartupHeartbeat() {
        long startedAt = System.nanoTime();
        
        ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
            .setNameFormat("Startup Heartbeat")
            .setDaemon(true)
            .build());
        
        // Every x seconds, ping something back to the frontend. If this stops, the app knows it's failed.
        heartbeat.scheduleAtFixedRate(
            () -> LOGGER.info("Still starting up... ({} elapsed)", MiscUtils.formatDuration(Duration.ofNanos(System.nanoTime() - startedAt))),
            10, 10, TimeUnit.SECONDS
        );
        
        return heartbeat;
    }

    private void pingPong() {
        this.services.threads().pingPongPool().scheduleAtFixedRate(() -> {
            try {
                this.services().rcpApi().sendMessage(new PingLauncherData());
            } catch (Exception ignored) {
                LOGGER.warn("Failed to send ping");
            }
        }, 0, 3, TimeUnit.SECONDS);
    }

    private final AtomicBoolean cleanedUp = new AtomicBoolean(false);

    /// Runs from exit() and again from the shutdown hook that System.exit fires, so only the first call does anything.
    public void cleanUpBeforeExit() {
        if (!cleanedUp.compareAndSet(false, true)) return;

        LOGGER.info("Cleaning up for shutdown");
        this.services.rcpApi().stop();
        this.services.threads().close();
    }

    public void exit() {
        cleanUpBeforeExit();
        System.exit(0);
    }

    /// Ends startup for a problem the app can't run with. Only call before the services are created
    private void failStartup(StartupFailure failure) {
        LOGGER.error("Backend Failed! Code={} Message={}", failure.name(), failure.message);
        LogManager.shutdown();
        System.exit(failure.exitCode);
    }

    private enum StartupFailure {
        DATA_DIR_NOT_WRITABLE(10, "The FTB App can't write to its data folder. This can be caused by file permissions, anti-virus or other configuration issues. Fix the folder's permissions, then start the app again.");

        private final int exitCode;
        private final String message;

        StartupFailure(int exitCode, String message) {
            this.exitCode = exitCode;
            this.message = message;
        }
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
        if (Files.exists(paths.processMarkerFile())) {
            try {
                var data = Files.readString(paths.processMarkerFile());
                var pid = Long.parseLong(data.trim());
                ProcessHandle.of(pid).ifPresent((handle) -> {
                    if (!isOwnExecutable(handle)) {
                        LOGGER.warn("Pid {} from app.pid file does not appear to belong to a previous instance of this app (likely reused by an unrelated process). Not terminating it.", pid);
                        return;
                    }
                    // SHUT IT DOWN!
                    LOGGER.info("Found running process with pid {}", pid);
                    stopOldProcess(handle);
                });
            } catch (Throwable ex) {
                LOGGER.warn("Failed to read app.pid file", ex);
            }
        }

        // Write the current pid to the file
        var currentPid = ProcessHandle.current().pid();
        try {
            Files.writeString(paths.processMarkerFile(), String.valueOf(currentPid));
        } catch (IOException ex) {
            LOGGER.error("Failed to write app.pid file", ex);
        }
    }

    /// Waits for the old process to actually exit
    private static void stopOldProcess(ProcessHandle handle) {
        handle.destroy();
        try {
            handle.onExit().get(10, TimeUnit.SECONDS);
            return;
        } catch (TimeoutException ex) {
            LOGGER.warn("Process {} didn't exit within 10 seconds, killing it.", handle.pid());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return;
        } catch (ExecutionException ex) {
            LOGGER.warn("Failed waiting for process {} to exit.", handle.pid(), ex);
        }

        handle.destroyForcibly();
        try {
            handle.onExit().get(5, TimeUnit.SECONDS);
        } catch (TimeoutException | ExecutionException ex) {
            LOGGER.error("Process {} is still running after being killed.", handle.pid());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
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
            this.flags.platform(),
            System.getProperty("os.name"),
            System.getProperty("os.version"),
            Architecture.current().name(),
            this.paths.workingDir().toAbsolutePath(),
            String.join(" ", startArgs.values()),
            jvmArgs
        );
    }
}

