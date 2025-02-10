package dev.ftb.app.pack;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import dev.ftb.app.Constants;
import dev.ftb.app.accounts.AccountManager;
import dev.ftb.app.accounts.MicrosoftProfile;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.instances.LaunchInstanceData;
import dev.ftb.app.install.tasks.DownloadTask;
import dev.ftb.app.install.tasks.InstallAssetsTask;
import dev.ftb.app.install.tasks.TaskProgressAggregator;
import dev.ftb.app.install.tasks.TaskProgressListener;
import dev.ftb.app.minecraft.jsons.AssetIndexManifest;
import dev.ftb.app.minecraft.jsons.VersionListManifest;
import dev.ftb.app.minecraft.jsons.VersionManifest;
import dev.ftb.app.minecraft.jsons.VersionManifest.AssetIndex;
import dev.ftb.app.util.StreamGobblerLog;
import net.covers1624.jdkutils.JavaInstall;
import net.covers1624.jdkutils.JavaVersion;
import net.covers1624.jdkutils.JdkInstallationManager.ProvisionRequest;
import net.covers1624.quack.io.IOUtils;
import net.covers1624.quack.maven.MavenNotation;
import net.covers1624.quack.net.httpapi.RequestListener;
import net.covers1624.quack.util.DataUtils;
import net.covers1624.quack.util.SneakyUtils.ThrowingConsumer;
import net.covers1624.quack.util.SneakyUtils.ThrowingRunnable;
import org.apache.commons.lang3.text.StrLookup;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static dev.ftb.app.minecraft.jsons.VersionManifest.LEGACY_ASSETS_VERSION;
import static dev.ftb.app.util.Log4jMarkers.*;
import static net.covers1624.quack.collection.ColUtils.iterable;
import static net.covers1624.quack.util.SneakyUtils.sneak;

/**
 * Responsible for launching a specific instance.
 * <p>
 */
public class InstanceLauncher {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();

    private static final List<String> TELEMETRY_ARGS = List.of("clientid", "auth_xuid"); // Tracking & XBox related
    
    private final Instance instance;
    private Phase phase = Phase.NOT_STARTED;

    private final List<VersionManifest> manifests = new ArrayList<>();
    private final List<Path> tempDirs = new LinkedList<>();
    @Nullable
    private Thread processThread;
    @Nullable
    private Process process;
    @Nullable
    private LogThread logThread;

    private boolean forceStopped;

    private final ProgressTracker progressTracker;
    private final List<ThrowingConsumer<LaunchContext, Throwable>> startTasks = new LinkedList<>();
    private final List<ThrowingRunnable<Throwable>> exitTasks = new LinkedList<>();

    private static final int NUM_STEPS = 7;

    public InstanceLauncher(Instance instance) {
        this.instance = instance;
        this.progressTracker = new ProgressTracker(this.instance);
    }

    /**
     * Adds a task to execute when this Instance starts.
     *
     * @param task The task.
     */
    public void withStartTask(ThrowingConsumer<LaunchContext, Throwable> task) {
        startTasks.add(task);
    }

    /**
     * Adds a task to execute when this Instance exits.
     *
     * @param task The task.
     */
    public void withExitTask(ThrowingRunnable<Throwable> task) {
        exitTasks.add(task);
    }

    /**
     * If the instance has already been started and is currently running.
     *
     * @return If the instance is already running.
     */
    public boolean isRunning() {
        return processThread != null;
    }

    /**
     * Get the current phase.
     *
     * @return The phase.
     */
    public Phase getPhase() {
        return phase;
    }

    /**
     * Attempt to start launching the configured instance.
     * <p>
     * It is illegal to call this method if {@link #isRunning()} returns true.
     *
     * @throws InstanceLaunchException If there was a direct error preparing the instance to be launched.
     */
    public synchronized void launch(CancellationToken token, @Nullable String offlineUsername) throws InstanceLaunchException {
        assert !isRunning();
        LOGGER.info("Attempting to launch instance {}({})", instance.getName(), instance.getUuid());
        setPhase(Phase.INITIALIZING);
        progressTracker.reset(NUM_STEPS);

        Path assetsDir = Constants.BIN_LOCATION.resolve("assets");
        Path versionsDir = Constants.BIN_LOCATION.resolve("versions");
        Path librariesDir = Constants.BIN_LOCATION.resolve("libraries");

        Set<String> features = new HashSet<>();
        if (instance.props.width != 0 && instance.props.height != 0) {
            features.add("has_custom_resolution");
        }

        Set<String> privateTokens = new HashSet<>();

        // This is run outside the future, as whatever is calling this method should immediately handle any errors
        // preparing the instance to be launched. It is not fun to propagate exceptions/errors across threads.
        ProcessBuilder builder = prepareProcess(token, offlineUsername, assetsDir, versionsDir, librariesDir, features, privateTokens);

        // Start thread.
        processThread = new Thread(() -> {
            try {
                try {
                    String logMessage = String.join(" ", builder.command());
                    for (String privateToken : privateTokens) {
                        logMessage = logMessage.replaceAll(privateToken, "*****");
                    }
                    LOGGER.info("Starting Minecraft with command '{}'", logMessage);
                    process = builder.start();
                } catch (IOException e) {
                    LOGGER.error("Failed to start minecraft process!", e);
                    setPhase(Phase.ERRORED);
                    WebSocketHandler.sendMessage(new LaunchInstanceData.Stopped(instance.getUuid(), "launch_failed", -1));
                    process = null;
                    processThread = null;
                    return;
                }
                setPhase(Phase.STARTED);

                // Start up the logging threads.
                logThread = new LogThread(IOUtils.makeParents(instance.getDir().resolve("logs/console.log")));
                Logger logger = LogManager.getLogger("Minecraft");
                StreamGobblerLog stdoutGobbler = new StreamGobblerLog()
                        .setName("Instance STDOUT log Gobbler")
                        .setInput(process.getInputStream())
                        .setOutput(message -> {
                            logThread.bufferMessage(message);
                            logger.info(MINECRAFT, message);
                        });
                StreamGobblerLog stderrGobbler = new StreamGobblerLog()
                        .setName("Instance STDERR log Gobbler")
                        .setInput(process.getErrorStream())
                        .setOutput(message -> {
                            logThread.bufferMessage(message);
                            logger.error(MINECRAFT, message);
                        });
                stdoutGobbler.start();
                stderrGobbler.start();
                logThread.start();

                process.onExit().thenRunAsync(() -> {
                    // Not strictly necessary, but exits the log threads faster.
                    stdoutGobbler.stop();
                    stderrGobbler.stop();
                    logThread.stop = true;
                    logThread.interrupt();
                });

                while (process.isAlive()) {
                    try {
                        process.waitFor();
                    } catch (InterruptedException ignored) {
                    }
                }
                int exit = process.exitValue();
                LOGGER.info("Minecraft exited with status code: " + exit);
                setPhase(exit != 0 ? Phase.ERRORED : Phase.STOPPED);
                WebSocketHandler.sendMessage(new LaunchInstanceData.Stopped(instance.getUuid(), exit != 0 && !forceStopped ? "errored" : "stopped", exit));
                forceStopped = false;
                process = null;
                processThread = null;
            } catch (Throwable t) {
                LOGGER.error("Minecraft thread exited with an unrecoverable error.", t);
                if (process != null) {
                    // Something is very broken, force kill minecraft and at least return to a recoverable state.
                    LOGGER.error("Force quitting Minecraft. Unrecoverable error occurred!");
                    process.destroyForcibly();
                    process = null;
                }
                processThread = null;
                setPhase(Phase.ERRORED);
                WebSocketHandler.sendMessage(new LaunchInstanceData.Stopped(instance.getUuid(), "internal_error", -1));
            }
        });
        processThread.setName("Instance Thread [" + THREAD_COUNTER.getAndIncrement() + "]");
        processThread.setDaemon(true);
        processThread.start();
    }

    /**
     * Triggers a force stop of the running instance.
     * <p>
     * Does nothing if the instance is not running.`
     */
    public void forceStop() {
        if (!isRunning()) return;

        Process process = this.process;
        if (process == null) return;
        LOGGER.info("Force quitting Minecraft..");
        forceStopped = true;
        process.destroyForcibly();
    }

    /**
     * Resets the instance to a runnable state.
     */
    public void reset() throws InstanceLaunchException {
        if (isRunning()) throw new InstanceLaunchException("Instance is currently running. Stop it first.");
        if (phase == Phase.NOT_STARTED) return;

        assert process == null;
        assert processThread == null;

        manifests.clear();
        startTasks.clear();
        exitTasks.clear();
        setPhase(Phase.NOT_STARTED);
    }

    public void setLogStreaming(boolean state) {
        if (logThread == null) return;

        logThread.setStreamingEnabled(state);
    }

    private void setPhase(Phase newPhase) {
        if (newPhase == Phase.STOPPED || newPhase == Phase.ERRORED) {
            onStopped();
        }
        LOGGER.info("Setting phase: {}", newPhase);
        phase = newPhase;
    }

    private void onStopped() {
        for (ThrowingRunnable<Throwable> exitTask : exitTasks) {
            try {
                exitTask.run();
            } catch (Throwable e) {
                LOGGER.error(NO_SENTRY, "Failed to execute exit task for instance {}({})", instance.getName(), instance.getUuid(), e);
                LOGGER.error(SENTRY_ONLY, "Failed to execute instance exit tasks.", e);
            }
        }

        for (Path tempDir : tempDirs) {
            if (Files.notExists(tempDir)) continue;
            LOGGER.info("Cleaning up temporary directory: {}", tempDir);
            try (Stream<Path> files = Files.walk(tempDir)) {
                files
                        .filter(Files::exists)
                        .sorted(Comparator.reverseOrder())
                        .forEach(sneak(Files::delete));
            } catch (IOException e) {
                LOGGER.warn("Failed to delete temp directory. Scheduling for app exit.");
                tempDir.toFile().deleteOnExit();
            }
        }
        tempDirs.clear();
    }

    private ProcessBuilder prepareProcess(CancellationToken token, String offlineUsername, Path assetsDir, Path versionsDir, Path librariesDir, Set<String> features, Set<String> privateTokens) throws InstanceLaunchException {
        try {
            progressTracker.startStep("Pre-Start Tasks"); // TODO locale support.
            Path gameDir = instance.getDir().toAbsolutePath();
            LaunchContext context = new LaunchContext();
            for (ThrowingConsumer<LaunchContext, Throwable> startTask : startTasks) {
                startTask.accept(context);
            }
            progressTracker.updateDesc("Pre-Start Tasks Complete");
            progressTracker.finishStep();

            token.throwIfCancelled();

            progressTracker.startStep("Validate Java Runtime");
            Path javaExecutable;
            if (instance.props.embeddedJre) {
                String javaTarget = instance.versionManifest.getTargetVersion("runtime");
                Path javaHome;
                if (javaTarget == null) {
                    LOGGER.warn("VersionManifest does not specify java runtime version. Falling back to Vanilla major version, latest.");
                    JavaVersion version = getJavaVersion();
                    javaHome = Constants.getJdkManager().provisionJdk(
                            new ProvisionRequest.Builder()
                                    .forVersion(version)
                                    .preferJRE(true)
                                    .downloadListener(progressTracker.requestListener())
                                    .build()
                    );
                } else {
                    javaHome = Constants.getJdkManager().provisionJdk(
                            new ProvisionRequest.Builder()
                                    .withSemver(javaTarget)
                                    .preferJRE(true)
                                    .downloadListener(progressTracker.requestListener())
                                    .build()
                    );
                }
                LOGGER.info("Java home: {}", javaHome);
                LOGGER.info("Java home: {}", javaHome.toString());
                LOGGER.info("Java home: {}", javaHome.toUri());
                LOGGER.info("Java home: {}", javaHome.toUri().toASCIIString());
                javaExecutable = JavaInstall.getJavaExecutable(javaHome, true);
            } else {
                javaExecutable = instance.props.jrePath;
            }
            LOGGER.info("Java executable: {}", javaExecutable);
            LOGGER.info("Java executable: {}", javaExecutable.toString());
            LOGGER.info("Java executable: {}", javaExecutable.toUri());
            LOGGER.info("Java executable: {}", javaExecutable.toUri().toASCIIString());
            progressTracker.updateDesc("Java Runtime Validated");
            progressTracker.finishStep();

            prepareManifests(token, versionsDir);

            token.throwIfCancelled();

            progressTracker.startStep("Validate assets");
            Pair<AssetIndex, AssetIndexManifest> assetPair = checkAssets(token, versionsDir);
            Path virtualAssets = buildVirtualAssets(assetPair.getLeft(), assetPair.getRight(), gameDir, assetsDir);
            progressTracker.updateDesc("Assets Validated");
            progressTracker.finishStep();

            token.throwIfCancelled();

            List<VersionManifest.Library> libraries = collectLibraries(features);
            // Mojang may change libraries mid version.

            progressTracker.startStep("Validate libraries");
            validateLibraries(token, librariesDir, libraries);
            progressTracker.updateDesc("Libraries Validated");
            progressTracker.finishStep();

            token.throwIfCancelled();

            progressTracker.startStep("Validate client");
            validateClient(token, versionsDir);
            progressTracker.updateDesc("Client Validated");
            progressTracker.finishStep();

            token.throwIfCancelled();

            progressTracker.startStep("Extract natives");
            Path nativesDir = versionsDir.resolve(instance.props.modLoader).resolve(instance.props.modLoader + "-natives-" + System.nanoTime());
            extractNatives(nativesDir, librariesDir, libraries);
            progressTracker.updateDesc("Natives Extracted");
            progressTracker.finishStep();

            token.throwIfCancelled();
            
            progressTracker.startStep("Prepare process");
            
            Map<String, String> subMap = new HashMap<>();
            MicrosoftProfile profile = AccountManager.get().getActiveProfile();
            if (offlineUsername != null || profile == null) {
                UUID uuid;
                String username;
                if (profile == null) {
                    uuid = new UUID(0, 0);
                    username = offlineUsername != null ? offlineUsername : "Player";
                } else {
                    uuid = profile.getUuid();
                    username = profile.getMinecraftName();
                }
                
                // Offline
                subMap.put("auth_player_name", username);
                subMap.put("auth_uuid", uuid.toString());
                subMap.put("user_type", "legacy");
                subMap.put("auth_access_token", "null");
                subMap.put("user_properties", "{}");
                subMap.put("auth_session", "null");
            } else {
                subMap.put("auth_player_name", profile.getMinecraftName());
                subMap.put("auth_uuid", profile.getUuid().toString());
                subMap.put("user_properties", "{}"); // TODO, we may need to provide this all the time.
                subMap.put("user_type", "msa");
                subMap.put("xuid", profile.getXstsUserHash());
                String accessToken = profile.getMinecraftAccessToken();
                String sessionToken = "token:" + accessToken + ":" + profile.getUuid().toString().replace("-", "");
                subMap.put("auth_session", sessionToken);
                subMap.put("auth_access_token", accessToken);
                privateTokens.add(sessionToken);
                privateTokens.add(accessToken);
            }

            subMap.put("version_name", instance.props.modLoader);
            subMap.put("game_directory", gameDir.toString());
            subMap.put("assets_root", assetsDir.toAbsolutePath().toString());
            subMap.put("game_assets", virtualAssets.toAbsolutePath().toString());
            subMap.put("assets_index_name", manifests.get(0).assets);
            subMap.put("version_type", manifests.get(0).type);

            subMap.put("launcher_name", "FTBApp");
            subMap.put("launcher_version", Constants.APPVERSION);
            subMap.put("primary_jar", getGameJar(versionsDir).toAbsolutePath().toString());
            subMap.put("memory", String.valueOf(instance.props.memory));

            subMap.put("resolution_width", String.valueOf(instance.props.width));
            subMap.put("resolution_height", String.valueOf(instance.props.height));

            subMap.put("natives_directory", nativesDir.toAbsolutePath().toString());
            List<Path> classpath = collectClasspath(librariesDir, versionsDir, libraries);
            subMap.put("classpath", classpath.stream().distinct().map(e -> e.toAbsolutePath().toString()).collect(Collectors.joining(File.pathSeparator)));
            subMap.put("classpath_separator", File.pathSeparator);
            subMap.put("library_directory", librariesDir.toAbsolutePath().toString());

            AssetIndexManifest.AssetObject icon = assetPair.getRight().objects.get("icons/minecraft.icns");
            if (icon != null) {
                Path path = Constants.BIN_LOCATION.resolve("assets")
                        .resolve("objects")
                        .resolve(icon.getPath());
                subMap.put("minecraft_icon", path.toAbsolutePath().toString());
            }

            StrSubstitutor sub = new StrSubstitutor(new StrLookup<>() {
                @Override
                public String lookup(String key) {
                    if (TELEMETRY_ARGS.contains(key)) {
                        return null;
                    }
                    
                    String value = subMap.get(key);
                    if (value == null) {
                        LOGGER.fatal("Unmapped token key '{}' in Minecraft arguments!! ", key);
                        return null;
                    }
                    return value;
                }
            });

            List<String> jvmArgs = VersionManifest.collectJVMArgs(manifests, features).stream()
                    .map(sub::replace)
                    .collect(Collectors.toList());
            List<String> progArgs = VersionManifest.collectProgArgs(manifests, features).stream()
                    .map(sub::replace)
                    .collect(Collectors.toList());

            List<String> command = new ArrayList<>(jvmArgs.size() + progArgs.size() + 2);
            command.addAll(context.shellArgs);
            command.add(javaExecutable.toAbsolutePath().toString());
            command.add(sub.replace("-Xmx${memory}M"));
            command.addAll(jvmArgs);
            command.addAll(context.extraJVMArgs);
            command.add("-Duser.language=en");
            command.add("-Duser.country=US");
            // This is literally only required for forge 1.20.3. No clue why, but it does fix the issue :tada:
            command.add("-DlibraryDirectory=" + librariesDir.toAbsolutePath());
            command.add(getMainClass());
            command.addAll(progArgs);
            command.addAll(context.extraProgramArgs);
            if (instance.props.fullscreen) {
                command.add("--fullscreen");
            }
            ProcessBuilder builder = new ProcessBuilder()
                    .directory(gameDir.toFile())
                    .command(command);

            Map<String, String> env = builder.environment();
            // Apparently this can override our passed in Java arguments.
            env.remove("_JAVA_OPTIONS");
            env.remove("JAVA_TOOL_OPTIONS");
            env.remove("JAVA_OPTIONS");

            progressTracker.updateDesc("Process Prepared");
            progressTracker.finishStep();
            
            return builder;
        } catch (Throwable ex) {
            if (ex instanceof CancellationToken.Cancellation cancellation) {
                throw cancellation;
            }
            throw new InstanceLaunchException("Failed to prepare instance '" + instance.getName() + "'(" + instance.getUuid() + ").", ex);
        }
    }

    private void prepareManifests(CancellationToken token, Path versionsDir) throws IOException, InstanceLaunchException {
        manifests.clear();
        VersionListManifest versions = VersionListManifest.update(versionsDir);
        Set<String> seen = new HashSet<>();
        String id = instance.props.modLoader;
        while (id != null) {
            token.throwIfCancelled();
            if (!seen.add(id)) throw new IllegalStateException("Circular VersionManifest reference. Root: " + instance.props.modLoader);
            LOGGER.info("Preparing manifest {}", id);
            VersionManifest manifest = versions.resolveOrLocal(versionsDir, id);
            if (manifest == null) {
                throw new InstanceLaunchException("Failed to prepare instance. Missing installed version " + id + ". Please validate/re-install.");
            }
            // Build in reverse order. First in list should be Minecraft's.
            manifests.add(0, manifest);
            id = manifest.inheritsFrom;
        }
    }

    private Pair<AssetIndex, AssetIndexManifest> checkAssets(CancellationToken token, Path versionsDir) throws IOException, InstanceLaunchException {
        assert !manifests.isEmpty();

        LOGGER.info("Updating assets..");
        VersionManifest manifest = manifests.get(0);
        AssetIndex index = manifest.assetIndex;
        if (index == null) {
            LOGGER.info("Old manifest with broken assets. Querying vanilla manifest for assets..");
            if (manifest.assets == null) {
                LOGGER.warn("Version '{}' does not have an assetIndex. Assuming Legacy. (Harvesting from {})", manifest.id, LEGACY_ASSETS_VERSION);
                index = VersionManifest.assetsFor(versionsDir, LEGACY_ASSETS_VERSION);
            } else {
                index = VersionManifest.assetsFor(versionsDir, manifest.assets);
            }
            if (index == null) {
                LOGGER.error("Unable to find assets for '{}'.", manifest.id);
                throw new InstanceLaunchException("Unable to prepare Legacy/Unknown assets for '" + manifest.id + "'.");
            }
        }

        InstallAssetsTask assetsTask = new InstallAssetsTask(index);
        if (!assetsTask.isRedundant()) {
            try {
                assetsTask.execute(token, progressTracker.listenerForStep(true));
            } catch (Throwable ex) {
                throw new IOException("Failed to execute asset update task.", ex);
            }
        }
        return Pair.of(index, assetsTask.getResult());
    }

    private Path buildVirtualAssets(AssetIndex index, AssetIndexManifest assetManifest, Path gameDir, Path assetsDir) throws IOException {
        Path objects = assetsDir.resolve("objects");
        Path virtual = assetsDir.resolve("virtual").resolve(index.getId());
        Path resourcesDir = gameDir.resolve("resources");

        if (assetManifest.virtual || assetManifest.mapToResources) {
            Path vAssets = assetManifest.virtual ? virtual : resourcesDir;
            LOGGER.info("Building virtual assets into {}..", vAssets);
            for (Map.Entry<String, AssetIndexManifest.AssetObject> entry : assetManifest.objects.entrySet()) {
                String name = entry.getKey();
                AssetIndexManifest.AssetObject object = entry.getValue();

                Path virtualPath = vAssets.resolve(name);
                Path objectPath = objects.resolve(object.getPath());
                if (Files.exists(objectPath)) {
                    Files.copy(objectPath, IOUtils.makeParents(virtualPath), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            return vAssets;
        }
        return virtual;
    }

    private void validateClient(CancellationToken token, Path versionsDir) throws IOException {
        VersionManifest vanillaManifest = manifests.get(0);
        DownloadTask task = vanillaManifest.getClientDownload(versionsDir, getClientId());
        if (task != null) {
            LOGGER.info("Validating client download for {}", vanillaManifest.id);
            task.execute(token, progressTracker.listenerForStep(true));
        }
    }

    private void validateLibraries(CancellationToken token, Path librariesDir, List<VersionManifest.Library> libraries) throws IOException {
        LOGGER.info("Validating minecraft libraries...");
        List<DownloadTask> tasks = new LinkedList<>();
        for (VersionManifest.Library library : libraries) {
            DownloadTask task = library.createDownloadTask(librariesDir, true);
            if (task != null && !task.isRedundant()) {
                tasks.add(task);
            }
        }

        long totalLen = tasks.stream()
                .mapToLong(e -> {
                    if (e.getValidation().expectedSize == -1) {
                        // Try and HEAD request the content length.
                        return DownloadTask.getContentLength(e.getUrl());
                    }
                    return e.getValidation().expectedSize;
                })
                .sum();

        TaskProgressListener rootListener = progressTracker.listenerForStep(true);
        rootListener.start(totalLen);
        TaskProgressAggregator progressAggregator = new TaskProgressAggregator(rootListener);
        if (!tasks.isEmpty()) {
            LOGGER.info("{} dependencies failed to validate or were missing.", tasks.size());
            for (DownloadTask task : tasks) {
                token.throwIfCancelled();
                LOGGER.info("Downloading {}", task.getUrl());
                task.execute(token, progressAggregator);
            }
        }
        rootListener.finish(progressAggregator.getProcessed());
        LOGGER.info("Libraries validated!");
    }

    private void extractNatives(Path nativesDir, Path librariesDir, List<VersionManifest.Library> libraries) throws IOException {
        LOGGER.info("Extracting natives...");
        tempDirs.add(nativesDir);
        VersionManifest.OS current = VersionManifest.OS.current();
        for (VersionManifest.Library library : libraries) {
            if (library.natives == null) continue;
            String classifier = library.natives.get(current);
            if (classifier == null) continue;
            MavenNotation notation = library.name.withClassifier(classifier);
            Path nativesJar = notation.toPath(librariesDir);
            if (Files.notExists(nativesJar)) {
                LOGGER.warn("Missing natives jar! " + nativesJar.toAbsolutePath());
                continue;
            }
            LOGGER.info(" Extracting from '{}'.", nativesJar);
            // TODO: move to NIO File system for zip?
            try (ZipFile zipFile = new ZipFile(nativesJar.toFile())) {
                for (ZipEntry entry : iterable(zipFile.entries())) {
                    if (entry.isDirectory()) continue;
                    if (library.extract != null && !library.extract.shouldExtract(entry.getName())) continue;
                    Path dest = nativesDir.resolve(entry.getName());
                    if (Files.notExists(dest.getParent())) {
                        Files.createDirectories(dest.getParent());
                    }
                    try (OutputStream out = Files.newOutputStream(dest)) {
                        IOUtils.copy(zipFile.getInputStream(entry), out);
                    }
                }
            }
        }
    }

    private String getMainClass() {
        return manifests.stream()
                .map(e -> e.mainClass)
                .reduce((a, b) -> b != null ? b : a) // Last on the list, gets priority.
                .orElseThrow(() -> new IllegalStateException("Version manifest chain does not have mainClass attribute??? uwot?"));
    }

    private JavaVersion getJavaVersion() {
        JavaVersion ret = JavaVersion.JAVA_1_8;
        for (VersionManifest e : manifests) {
            JavaVersion parse = e.getJavaVersionOrDefault(null);
            if (parse == null) continue;

            if (parse.ordinal() > ret.ordinal()) {
                ret = parse;
            }
        }
        return ret;
    }

    private List<VersionManifest.Library> collectLibraries(Set<String> features) {
        // Reverse list, as last on manifest list gets put on the classpath first.
        return Lists.reverse(manifests)
                .stream()
                .flatMap(e -> e.getLibraries(features))
                .collect(Collectors.toList());
    }

    private List<Path> collectClasspath(Path librariesDir, Path versionsDir, List<VersionManifest.Library> libraries) {        
        List<Path> classpath = createUniqueLibraryList(libraries).stream()
                .filter(e -> e.natives == null)
                .map(e -> e.name.toPath(librariesDir))
                .collect(Collectors.toList());
        
        classpath.add(getGameJar(versionsDir));        
        return classpath;
    }
    
    private boolean neoRequiresDeDupeLibraries() {
        var neoForgeLibraryId = getLibraryIdFromName("neoforge", 1);
        if (neoForgeLibraryId == null) {
            return false;
        }

        // Modern neo is any version higher than 20.1, this is a safe enough check as it's never going to start with 20.1 if it's higher.
        return !neoForgeLibraryId.startsWith("20.1");
    }
    
    private boolean forgeRequiresDeDupeLibraries() {
        // We want the first part of the forge ID
        var forgeLibraryId = getLibraryIdFromName("forge", 0);
        if (forgeLibraryId == null) {
            return false;
        }
        
        if (!forgeLibraryId.startsWith("1.")) {
            return false;
        }
        
        // Split up the minecraft version
        var partedNumbers = Arrays.stream(forgeLibraryId.split("\\.")).map(Integer::parseInt).toList();
        if (partedNumbers.size() < 2) {
            return false;
        }
        
        // Anything newer than 1.21.3 requires deduping.
        if (partedNumbers.size() == 2) {
            return partedNumbers.get(1) > 21;
        }
        
        return partedNumbers.get(1) >= 21 && partedNumbers.get(2) > 3;
    }
    
    @Nullable
    private String getLibraryIdFromName(String name, int requestedIndex) {
        var foundName = manifests.stream()
            .map(e -> e.id)
            .filter(e -> e.contains(name))
            .findFirst();

        if (foundName.isEmpty()) {
            return null;
        }

        var parted = foundName.get().split("-");
        if (parted.length < requestedIndex) {
            return null;
        }
        
        return parted[requestedIndex];
    }
    
    private List<VersionManifest.Library> createUniqueLibraryList(List<VersionManifest.Library> libraries) {
        if (!neoRequiresDeDupeLibraries() && !forgeRequiresDeDupeLibraries()) {
            return libraries;
        }
        
        // Deduplicate libraries
        List<VersionManifest.Library> uniqueLibraries = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Set<String> handled = new HashSet<>();
        for (VersionManifest.Library library : libraries) {
            var name = createVersionAgnosticMavenString(library);

            if (seen.add(name)) {
                uniqueLibraries.add(library);
            } else {
                if (handled.contains(name)) {
                    continue;
                }

                // It looks like the lowest versions are always first in the list.
                LOGGER.info("Duplicate library found: {} ({})", name, library.name);

                VersionManifest.Library lowest = null;
                Set<VersionManifest.Library> possibleOptions = new HashSet<>();
                for (VersionManifest.Library lib : libraries) {
                    // Find the higher version of the library.
                    var libName = createVersionAgnosticMavenString(lib);
                    if (libName.equals(name)) {
                        possibleOptions.add(lib);
                        // Now we need to find the higher version.
                        if (lowest != null) {
                            continue;
                        }
                        
                        lowest = lib;
                    }
                }

                if (lowest != null) {
                    uniqueLibraries.remove(library);
                    uniqueLibraries.add(lowest);
                    LOGGER.info("Using lowest version of library: {} ({}). Selected from:", name, lowest.name);
                    for (VersionManifest.Library lib : possibleOptions) {
                        LOGGER.info("\t- {}", lib.name);
                    }
                }

                handled.add(name);
            }
        }
        
        return uniqueLibraries;
    }
    
    private String createVersionAgnosticMavenString(VersionManifest.Library library) {
        var name = library.name.group + ":" + library.name.module;
        if (library.name.classifier != null) {
            name += ":" + library.name.classifier;
        }
        
        name += "@" + library.name.extension;
        return name;
    }

    private Path getGameJar(Path versionsDir) {
        String id = getClientId();
        return versionsDir.resolve(id).resolve(id + ".jar");
    }

    private String getClientId() {
        for (VersionManifest manifest : Lists.reverse(manifests)) {
            if (manifest.jar != null) {
                return manifest.jar;
            }
        }
        return manifests.get(manifests.size() - 1).id;
    }

    public static class LaunchContext {
        public final List<String> extraJVMArgs = new ArrayList<>();
        public final List<String> extraProgramArgs = new ArrayList<>();
        public final List<String> shellArgs = new ArrayList<>();
    }

    public enum Phase {
        /**
         * The Instance has not been started.
         */
        NOT_STARTED,
        /**
         * The Instance is initializing.
         * - Setting up instance.
         * - Downloading Minecraft assets.
         */
        INITIALIZING,
        /**
         * The Instance has been started.
         */
        STARTED,
        /**
         * The Instance has been stopped.
         */
        STOPPED,
        /**
         * The instance errored.
         * <p>
         * This could mean it exited with a non-zero exit code.
         * Or the process failed to start.
         */
        ERRORED,
    }

    private static class ProgressTracker {
        private static final boolean DEBUG = Boolean.getBoolean("InstanceLauncher.ProgressTracker.debug");

        private Instance instance;
        
        private int totalSteps = 0;
        private int currStep = 0;

        private String stepDesc = "";

        private float stepProgress;
        private String humanDesc = null;
        private long lastNonImportant = -1;

        public ProgressTracker(Instance instance) {
            this.instance = instance;
        }

        public void reset(int totalSteps) {
            this.totalSteps = totalSteps;
            currStep = 0;
            stepDesc = "";
            stepProgress = 0.0F;
        }

        public void startStep(String stepDesc) {
            currStep++;
            this.stepDesc = stepDesc;
            humanDesc = null;
            sendUpdate(true);
        }

        public void updateDesc(String desc) {
            this.stepDesc = desc;
            sendUpdate(true);
        }

        public TaskProgressListener listenerForStep(boolean isDownload) {
            return new TaskProgressListener() {
                private long total;

                @Override
                public void start(long total) {
                    this.total = total;
                }

                @Override
                public void update(long processed) {
                    stepProgress = (float) ((double) processed / (double) total);
                    if (stepProgress == Float.NEGATIVE_INFINITY || stepProgress == Float.POSITIVE_INFINITY) {
                        // Wat?
                        stepProgress = 0;
                    }
                    if (isDownload) {
                        humanDesc = DataUtils.humanSize(processed) + " / " + DataUtils.humanSize(total);
                    }
                    sendUpdate(false);
                }

                @Override
                public void finish(long total) {
                }
            };
        }

        public RequestListener requestListener() {
            return new RequestListener() {
                @Override
                public void start(Direction type) { }

                @Override
                public void onUpload(long total, long now) {
                }

                @Override
                public void onDownload(long total, long now) {
                    stepProgress = (float) ((double) now / (double) total);
                    if (stepProgress == Float.NEGATIVE_INFINITY || stepProgress == Float.POSITIVE_INFINITY) {
                        // Wat?
                        stepProgress = 0;
                    }
                    humanDesc = DataUtils.humanSize(now) + " / " + DataUtils.humanSize(total);
                    sendUpdate(false);
                }

                @Override
                public void end() {

                }
            };
        }

        public void finishStep() {
            stepProgress = 1.0F;
            sendUpdate(true);
        }

        private void sendUpdate(boolean important) {
            if (!important) {
                // Rate limit non-important messages to every 100 millis
                if (lastNonImportant != -1 && System.currentTimeMillis() - 100 < lastNonImportant) {
                    return;
                }
                lastNonImportant = System.currentTimeMillis();
            } else {
                lastNonImportant = -1;
            }
            if (DEBUG) {
                LOGGER.info("Progress [{}/{}] {}: {} {}", currStep, totalSteps, stepProgress, stepDesc, humanDesc);
            }

            WebSocketHandler.sendMessage(new LaunchInstanceData.Status(this.instance.getUuid(), currStep, totalSteps, stepProgress, stepDesc, humanDesc));
        }
    }

    private class LogThread extends Thread {

        private static final boolean DEBUG = Boolean.getBoolean("InstanceLauncher.LogThread.debug");
        /**
         * The time in milliseconds between bursts of logging output.
         */
        private static final long INTERVAL = 250;

        private boolean streamingEnabled = true;

        private boolean stop = false;
        private final List<String> pendingMessages = new ArrayList<>(100);
        @Nullable
        private final PrintStream pw;

        public LogThread(Path path) {
            super("Instance Logging Thread");
            setDaemon(true);

            PrintStream pw = null;
            try {
                pw = new PrintStream(Files.newOutputStream(path), true);
            } catch (IOException ex) {
                LOGGER.error("Failed to create console log for instance {}.", instance.getUuid(), ex);
            }
            this.pw = pw;
        }

        @Override
        @SuppressWarnings ("BusyWait")
        public void run() {
            while (!stop && phase == Phase.STARTED) {
                if (!pendingMessages.isEmpty()) {
                    synchronized (pendingMessages) {
                        List<String> toSend = ImmutableList.copyOf(pendingMessages);
                        pendingMessages.clear();
                        if (DEBUG) {
                            LOGGER.info("Flushing {} messages.", toSend.size());
                        }

                        WebSocketHandler.sendMessage(new LaunchInstanceData.Logs(instance.getUuid(), toSend));
                    }
                }
                try {
                    Thread.sleep(INTERVAL);
                } catch (InterruptedException ignored) {
                    if (Thread.interrupted()) {
                        if (stop) {
                            break;
                        }
                    }
                }
            }
            if (pw != null) {
                pw.close();
            }
        }

        private void bufferMessage(String message) {
            if (streamingEnabled) {
                synchronized (pendingMessages) {
                    pendingMessages.add(message);
                }
            }
            if (pw != null) {
                pw.println(message);
            }
        }

        public void setStreamingEnabled(boolean state) {
            streamingEnabled = state;
            if (!state) {
                synchronized (pendingMessages) {
                    pendingMessages.clear();
                }
            }
        }
    }
}
