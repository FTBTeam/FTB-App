package net.creeperhost.creeperlauncher.pack;

import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;
import net.covers1624.jdkutils.JavaInstall;
import net.covers1624.jdkutils.JavaVersion;
import net.covers1624.quack.io.IOUtils;
import net.covers1624.quack.maven.MavenNotation;
import net.covers1624.quack.util.DataUtils;
import net.covers1624.quack.util.SneakyUtils.ThrowingConsumer;
import net.covers1624.quack.util.SneakyUtils.ThrowingRunnable;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.instances.LaunchInstanceData;
import net.creeperhost.creeperlauncher.install.tasks.InstallAssetsTask;
import net.creeperhost.creeperlauncher.install.tasks.NewDownloadTask;
import net.creeperhost.creeperlauncher.install.tasks.TaskProgressAggregator;
import net.creeperhost.creeperlauncher.install.tasks.TaskProgressListener;
import net.creeperhost.creeperlauncher.minecraft.account.AccountManager;
import net.creeperhost.creeperlauncher.minecraft.account.AccountProfile;
import net.creeperhost.creeperlauncher.minecraft.jsons.VersionListManifest;
import net.creeperhost.creeperlauncher.minecraft.jsons.VersionManifest;
import net.creeperhost.creeperlauncher.util.GsonUtils;
import net.creeperhost.creeperlauncher.util.QuackProgressAdapter;
import net.creeperhost.creeperlauncher.util.StreamGobblerLog;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.util.Objects.requireNonNull;
import static net.covers1624.quack.collection.ColUtils.iterable;
import static net.covers1624.quack.util.SneakyUtils.sneak;
import static net.creeperhost.creeperlauncher.install.tasks.NewDownloadTask.DownloadValidation;

/**
 * Responsible for launching a specific instance.
 * <p>
 * Created by covers1624 on 17/11/21.
 */
public class InstanceLauncher {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();

    private final LocalInstance instance;
    private Phase phase = Phase.NOT_STARTED;

    private final List<VersionManifest> manifests = new ArrayList<>();
    private final List<Path> tempDirs = new LinkedList<>();
    @Nullable
    private Thread processThread;
    @Nullable
    private Process process;

    private final ProgressTracker progressTracker = new ProgressTracker();
    private final List<ThrowingConsumer<LaunchContext, IOException>> startTasks = new LinkedList<>();
    private final List<ThrowingRunnable<IOException>> exitTasks = new LinkedList<>();

    private static final int NUM_STEPS = 5;

    public InstanceLauncher(LocalInstance instance) {
        this.instance = instance;
    }

    /**
     * Adds a task to execute when this Instance starts.
     *
     * @param task The task.
     */
    public void withStartTask(ThrowingConsumer<LaunchContext, IOException> task) {
        startTasks.add(task);
    }

    /**
     * Adds a task to execute when this Instance exits.
     *
     * @param task The task.
     */
    public void withExitTask(ThrowingRunnable<IOException> task) {
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
    public synchronized void launch(int requestId) throws InstanceLaunchException {
        assert !isRunning();
        LOGGER.info("Attempting to launch instance {}({})", instance.getName(), instance.getUuid());
        setPhase(Phase.INITIALIZING);
        progressTracker.reset(requestId, NUM_STEPS);

        Path assetsDir = Constants.BIN_LOCATION.resolve("assets");
        Path versionsDir = Constants.BIN_LOCATION.resolve("versions");
        Path librariesDir = Constants.BIN_LOCATION.resolve("libraries");

        Set<String> features = new HashSet<>();
        if (instance.width != 0 && instance.height != 0) {
            features.add("has_custom_resolution");
        }

        // This is run outside the future, as whatever is calling this method should immediately handle any errors
        // preparing the instance to be launched. It is not fun to propagate exceptions/errors across threads.
        ProcessBuilder builder = prepareProcess(assetsDir, versionsDir, librariesDir, features);

        // Start thread.
        processThread = new Thread(() -> {
            try {
                try {
                    LOGGER.info("Starting Minecraft with command '{}'", String.join(" ", builder.command()));
                    process = builder.start();
                } catch (IOException e) {
                    LOGGER.error("Failed to start minecraft process!", e);
                    setPhase(Phase.ERRORED);
                    process = null;
                    processThread = null;
                    return;
                }
                setPhase(Phase.STARTED);
                Logger logger = LogManager.getLogger("Minecraft Temp");
                CompletableFuture<Void> stdoutFuture = StreamGobblerLog.redirectToLogger(process.getInputStream(), logger::info);
                CompletableFuture<Void> stderrFuture = StreamGobblerLog.redirectToLogger(process.getErrorStream(), logger::error);
                process.onExit().thenRunAsync(() -> {
                    if (!stdoutFuture.isDone()) {
                        stdoutFuture.cancel(true);
                    }
                    if (!stderrFuture.isDone()) {
                        stderrFuture.cancel(true);
                    }
                });

                while (process.isAlive()) {
                    try {
                        process.waitFor();
                    } catch (InterruptedException ignored) {
                    }
                }
                int exit = process.exitValue();
                setPhase(exit != 0 ? Phase.ERRORED : Phase.STOPPED);
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

    private void setPhase(Phase newPhase) {
        if (newPhase == Phase.STOPPED || newPhase == Phase.ERRORED) {
            onStopped();
        }
        LOGGER.info("Setting phase: {}", newPhase);
        phase = newPhase;
    }

    private void onStopped() {
        for (ThrowingRunnable<IOException> exitTask : exitTasks) {
            try {
                exitTask.run();
            } catch (IOException e) {
                LOGGER.error("Failed to execute exit task for instance {}({})", instance.getName(), instance.getUuid(), e);
            }
        }

        for (Path tempDir : tempDirs) {
            if (Files.notExists(tempDir)) continue;
            LOGGER.info("Cleaning up temporary directory: {}", tempDir);
            try {
                Files.walk(tempDir)
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

    private ProcessBuilder prepareProcess(Path assetsDir, Path versionsDir, Path librariesDir, Set<String> features) throws InstanceLaunchException {
        try {
            progressTracker.startStep("Pre-Start Tasks"); // TODO locale support.
            Path gameDir = instance.getDir().toAbsolutePath();
            LaunchContext context = new LaunchContext();
            for (ThrowingConsumer<LaunchContext, IOException> startTask : startTasks) {
                startTask.accept(context);
            }
            progressTracker.finishStep();

            progressTracker.startStep("Validate Java Runtime");
            Path javaExecutable;
            if (instance.embeddedJre) {
                Path javaHome = Constants.JDK_INSTALL_MANAGER.provisionJdk(getJavaVersion(), new QuackProgressAdapter(progressTracker.listenerForStep(true)));
                javaExecutable = JavaInstall.getJavaExecutable(javaHome, true);
            } else {
                javaExecutable = instance.jrePath;
            }
            progressTracker.finishStep();

            prepareManifests(versionsDir);

            progressTracker.startStep("Validate assets");
            checkAssets();
            progressTracker.finishStep();

            List<VersionManifest.Library> libraries = collectLibraries(features);
            // Mojang may change libraries mid version.

            progressTracker.startStep("Validate libraries");
            validateLibraries(librariesDir, libraries);
            progressTracker.finishStep();

            progressTracker.startStep("Validate client");
            validateClient(versionsDir);
            progressTracker.finishStep();

            Path nativesDir = versionsDir.resolve(instance.modLoader).resolve(instance.modLoader + "-natives-" + System.nanoTime());
            extractNatives(nativesDir, librariesDir, libraries);

            Map<String, String> subMap = new HashMap<>();
            AccountProfile profile = AccountManager.get().getActiveProfile();
            if (profile == null) {
                // Offline
                subMap.put("auth_player_name", "Player"); // TODO, give user ability to set name?
                subMap.put("auth_uuid", new UUID(0, 0).toString());
                subMap.put("user_type", "legacy");
                subMap.put("auth_access_token", "null");
                subMap.put("user_properties", "{}");
            } else {
                subMap.put("auth_player_name", profile.username);
                subMap.put("auth_uuid", profile.uuid.toString());
                subMap.put("user_properties", "{}"); // TODO, we may need to provide this all the time.
                if (profile.msAuth != null) {
                    subMap.put("user_type", "msa");
                    subMap.put("auth_access_token", profile.msAuth.minecraftToken);
                    subMap.put("xuid", profile.msAuth.xblUserHash);
                } else {
                    assert profile.mcAuth != null;
                    subMap.put("user_type", "mojang");
                    subMap.put("auth_access_token", profile.mcAuth.accessToken);
                }
            }

            subMap.put("version_name", instance.modLoader);
            subMap.put("game_directory", gameDir.toString());
            subMap.put("assets_root", assetsDir.toAbsolutePath().toString());
            subMap.put("assets_index_name", manifests.get(0).assets);
            subMap.put("version_type", manifests.get(0).type);

            subMap.put("launcher_name", "FTBApp");
            subMap.put("launcher_version", Constants.APPVERSION);
            subMap.put("primary_jar", getGameJar(versionsDir).toAbsolutePath().toString());
            subMap.put("memory", String.valueOf(instance.memory));

            subMap.put("resolution_width", String.valueOf(instance.width));
            subMap.put("resolution_height", String.valueOf(instance.height));

            subMap.put("natives_directory", nativesDir.toAbsolutePath().toString());
            List<Path> classpath = collectClasspath(librariesDir, versionsDir, libraries);
            subMap.put("classpath", classpath.stream().distinct().map(e -> e.toAbsolutePath().toString()).collect(Collectors.joining(File.pathSeparator)));

            StrSubstitutor sub = new StrSubstitutor(subMap);

            List<String> jvmArgs = VersionManifest.collectJVMArgs(manifests, features).stream()
                    .map(sub::replace)
                    .collect(Collectors.toList());
            List<String> progArgs = VersionManifest.collectProgArgs(manifests, features).stream()
                    .map(sub::replace)
                    .collect(Collectors.toList());

            List<String> command = new ArrayList<>(jvmArgs.size() + progArgs.size() + 2);
            command.add(javaExecutable.toAbsolutePath().toString());
            command.addAll(jvmArgs);
            command.addAll(context.extraJVMArgs);
            // TODO, these should be the defaults inside the app, not added here.
            for (String arg : Constants.MOJANG_DEFAULT_ARGS) {
                command.add(sub.replace(arg));
            }
            command.add(getMainClass());
            command.addAll(progArgs);
            command.addAll(context.extraProgramArgs);
            return new ProcessBuilder()
                    .directory(gameDir.toFile())
                    .command(command);
        } catch (Throwable ex) {
            throw new InstanceLaunchException("Failed to prepare instance '" + instance.getName() + "'(" + instance.getUuid() + ").", ex);
        }
    }

    private void prepareManifests(Path versionsDir) throws IOException {
        manifests.clear();
        VersionListManifest versions = VersionListManifest.update(versionsDir);
        Set<String> seen = new HashSet<>();
        String id = instance.modLoader;
        while (id != null) {
            if (!seen.add(id)) throw new IllegalStateException("Circular VersionManifest reference. Root: " + instance.modLoader);
            LOGGER.info("Preparing manifest {}", id);
            VersionManifest manifest = getVersionManifest(versionsDir, versions, id);
            // Build in reverse order. First in list should be Minecraft's.
            manifests.add(0, manifest);
            id = manifest.inheritsFrom;
        }
    }

    private void checkAssets() throws IOException {
        assert !manifests.isEmpty();

        LOGGER.info("Updating assets..");
        VersionManifest manifest = manifests.get(0);
        InstallAssetsTask assetsTask = new InstallAssetsTask(requireNonNull(manifest.assetIndex, "First Version Manifest missing AssetIndex. This should not happen."));
        if (assetsTask.isRedundant()) return;
        try {
            assetsTask.execute(progressTracker.listenerForStep(true));
        } catch (Throwable ex) {
            throw new IOException("Failed to execute asset update task.", ex);
        }
    }

    private void validateClient(Path versionsDir) throws IOException {
        VersionManifest manifest = manifests.get(0);
        VersionManifest.Download download = manifest.downloads.get("client");
        if (download != null && download.url != null) {
            LOGGER.info("Validating client download for {}", manifest.id);
            DownloadValidation validation = DownloadValidation.of()
                    .withExpectedSize(download.size);
            if (download.sha1 != null) {
                validation = validation.withHash(Hashing.sha1(), download.sha1);
            }
            NewDownloadTask task = new NewDownloadTask(
                    download.url,
                    versionsDir.resolve(manifest.id).resolve(manifest.id + ".jar"),
                    validation
            );
            task.execute(progressTracker.listenerForStep(true));
        }
    }

    private void validateLibraries(Path librariesDir, List<VersionManifest.Library> libraries) throws IOException {
        LOGGER.info("Validating minecraft libraries...");
        List<NewDownloadTask> tasks = new LinkedList<>();
        for (VersionManifest.Library library : libraries) {
            NewDownloadTask task = library.createDownloadTask(librariesDir);
            if (task != null && !task.isRedundant()) {
                tasks.add(task);
            }
        }

        long totalLen = tasks.stream()
                .mapToLong(e -> {
                    if (e.getValidation().expectedSize() == -1) {
                        // Try and HEAD request the content length.
                        return getContentLength(e.getUrl());
                    }
                    return e.getValidation().expectedSize();
                })
                .sum();

        TaskProgressListener rootListener = progressTracker.listenerForStep(true);
        rootListener.start(totalLen);
        TaskProgressAggregator progressAggregator = new TaskProgressAggregator(rootListener);
        if (!tasks.isEmpty()) {
            LOGGER.info("{} dependencies failed to validate or were missing.", tasks.size());
            for (NewDownloadTask task : tasks) {
                LOGGER.info("Downloading {}", task.getUrl());
                task.execute(progressAggregator);
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
            VersionManifest.JavaVersion javaVersion = e.javaVersion;
            if (javaVersion == null) continue;
            JavaVersion parse = JavaVersion.parse(String.valueOf(javaVersion.majorVersion));
            if (parse == null || parse == JavaVersion.UNKNOWN) {
                LOGGER.error("Unable to parse '{}' into a JavaVersion.", javaVersion.majorVersion);
                continue;
            }
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
        List<Path> classpath = libraries.stream()
                .filter(e -> e.natives == null)
                .map(e -> e.name.toPath(librariesDir))
                .collect(Collectors.toList());
        classpath.add(getGameJar(versionsDir));
        return classpath;
    }

    private Path getGameJar(Path versionsDir) {
        String rootId = manifests.get(0).id;
        return versionsDir.resolve(rootId).resolve(rootId + ".jar");
    }

    private static VersionManifest getVersionManifest(Path versionsFolder, VersionListManifest versions, String id) throws IOException {
        VersionListManifest.Version version = versions.locate(id);
        if (version == null) {
            LOGGER.info("Version {} not found on remote list, trying locally.", id);
            Path versionJson = versionsFolder.resolve(id).resolve(id + ".json");
            if (Files.notExists(versionJson)) throw new FileNotFoundException("Unable to find version json for: '" + id + "'. Searched: '" + versionJson + "'.");
            return GsonUtils.loadJson(versionJson, VersionManifest.class);
        }
        return VersionManifest.update(versionsFolder, version);
    }

    private static long getContentLength(String url) {
        Request request = new Request.Builder()
                .head()
                .url(url)
                .build();
        try (Response response = NewDownloadTask.client.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (body != null) return body.contentLength();

            return NumberUtils.toInt(response.header("Content-Length"));
        } catch (IOException e) {
            LOGGER.error("Could not perform a HEAD request to {}", url);
        }
        return 0;
    }

    public static class LaunchContext {

        public final List<String> extraJVMArgs = new ArrayList<>();
        public final List<String> extraProgramArgs = new ArrayList<>();
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

        private int requestId;

        private int totalSteps = 0;
        private int currStep = 0;

        private String stepDesc = "";

        private float stepProgress;
        private String humanDesc = null;
        private long lastNonImportant = -1;

        public void reset(int requestId, int totalSteps) {
            this.requestId = requestId;
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

            if (Settings.webSocketAPI == null) return;
            Settings.webSocketAPI.sendMessage(new LaunchInstanceData.Status(requestId, currStep, totalSteps, stepProgress, stepDesc, humanDesc));
        }
    }
}