package net.creeperhost.creeperlauncher.pack;

import com.google.common.collect.Lists;
import net.covers1624.quack.io.IOUtils;
import net.covers1624.quack.maven.MavenNotation;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.install.tasks.InstallAssetsTask;
import net.creeperhost.creeperlauncher.install.tasks.Task;
import net.creeperhost.creeperlauncher.minecraft.jsons.VersionListManifest;
import net.creeperhost.creeperlauncher.minecraft.jsons.VersionManifest;
import net.creeperhost.creeperlauncher.util.GsonUtils;
import net.creeperhost.creeperlauncher.util.StreamGobblerLog;
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
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.util.Objects.requireNonNull;
import static net.covers1624.quack.collection.ColUtils.iterable;
import static net.covers1624.quack.util.SneakyUtils.sneak;

/**
 * Responsible for launching a specific instance.
 * <p>
 * Created by covers1624 on 17/11/21.
 */
public class InstanceLauncher {

    private static final Logger LOGGER = LogManager.getLogger();

    private final LocalInstance instance;
    private Phase phase = Phase.NOT_STARTED;

    private final List<VersionManifest> manifests = new ArrayList<>();
    private final List<Path> tempDirs = new LinkedList<>();
    @Nullable
    private CompletableFuture<Void> processFuture;
    @Nullable
    private Process process;

    public static void main(String[] args) throws Throwable {
        Instances.refreshInstances();
        LocalInstance instance = Instances.getInstance(UUID.fromString("5cf33a16-9f4d-4fbc-bc3b-f27c6a96e185"));
        InstanceLauncher launcher = new InstanceLauncher(instance);

        launcher.launch();
        synchronized (launcher) {
            launcher.wait();
        }
    }

    public InstanceLauncher(LocalInstance instance) {
        this.instance = instance;
    }

    /**
     * If the instance has already been started and is currently running.
     *
     * @return If the instance is already running.
     */
    public boolean isRunning() {
        return processFuture != null;
    }

    /**
     * Attempt to start launching the configured instance.
     * <p>
     * It is illegal to call this method if {@link #isRunning()} returns true.
     *
     * @throws LaunchException If there was a direct error preparing the instance to be launched.
     */
    public synchronized void launch() throws LaunchException {
        assert !isRunning();
        LOGGER.info("Attempting to launch instance {}({})", instance.getName(), instance.getUuid());
        setPhase(Phase.INITIALIZING);

        Path assetsDir = Constants.BIN_LOCATION.resolve("assets");
        Path versionsDir = Constants.BIN_LOCATION.resolve("versions");
        Path librariesDir = Constants.BIN_LOCATION.resolve("libraries");

        Set<String> features = Set.of();

        // This is run outside the future, as whatever is calling this method should immediately handle any errors
        // preparing the instance to be launched. It is not fun to propagate exceptions/errors across threads.
        ProcessBuilder builder = prepareProcess(assetsDir, versionsDir, librariesDir, features);

        // Spawn future to immediately start minecraft
        processFuture = CompletableFuture.runAsync(() -> {
            try {
                LOGGER.info("Starting Minecraft with command '{}'", String.join(" ", builder.command()));
                process = builder.start();
            } catch (IOException e) {
                LOGGER.error("Failed to start minecraft process!", e);
                setPhase(Phase.ERRORED);
                process = null;
                processFuture = null;
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
            processFuture = null;
        }).exceptionally(t -> {
            LOGGER.error("Minecraft Future exited with an unrecoverable error.", t);
            if (process != null) {
                // Something is very broken, force kill minecraft and at least return to a recoverable state.
                LOGGER.error("Force quitting Minecraft. Unrecoverable error occurred!");
                process.destroyForcibly();
                process = null;
            }
            processFuture = null;
            setPhase(Phase.ERRORED);
            return null;
        });
    }

    private void setPhase(Phase newPhase) {
        if (newPhase == Phase.STOPPED || newPhase == Phase.ERRORED) {
            onStopped();
        }
        LOGGER.info("Setting phase: {}", newPhase);
        phase = newPhase;
    }

    private void onStopped() {
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

    private ProcessBuilder prepareProcess(Path assetsDir, Path versionsDir, Path librariesDir, Set<String> features) throws LaunchException {
        Path javaExecutable = Paths.get("/usr/lib/jvm/java-8-openjdk/bin/java"); // TODO
        Path gameDir = instance.getDir().toAbsolutePath();
        try {
            // TODO JDK download stuffs. (should also be done on install, but as a fallback done here too if they don't exist (maybe a migrator?))

            prepareManifests(versionsDir);
            // TODO, may need UI feedback. We will run this once during instance install, but we need to refresh them here too.
            checkAssets();

            String mainClass = getMainClass();
            List<VersionManifest.Library> libraries = collectLibraries(features);

            Path nativesDir = versionsDir.resolve(instance.modLoader).resolve(instance.modLoader + "-natives-" + System.nanoTime());
            extractNatives(nativesDir, librariesDir, libraries);

            Map<String, String> subMap = new HashMap<>();
            // Temp properties TODO
            subMap.put("auth_player_name", "Player");
            subMap.put("auth_uuid", new UUID(0, 0).toString());
            subMap.put("user_type", "legacy");
            subMap.put("auth_access_token", "null"); // TODO

            subMap.put("version_name", instance.modLoader);
            subMap.put("game_directory", gameDir.toString());
            subMap.put("assets_root", assetsDir.toAbsolutePath().toString());
            subMap.put("assets_index_name", manifests.get(0).assets);
            subMap.put("version_type", manifests.get(0).type);// TODO

            subMap.put("launcher_name", "FTBApp");
            subMap.put("launcher_version", "6.9.4.20");// TODO

            subMap.put("natives_directory", nativesDir.toAbsolutePath().toString());
            List<Path> classpath = collectClasspath(librariesDir, versionsDir, libraries);
            subMap.put("classpath", classpath.stream().map(e -> e.toAbsolutePath().toString()).collect(Collectors.joining(File.pathSeparator)));

            StrSubstitutor sub = new StrSubstitutor(subMap);

            List<String> jvmArgs = collectArgs(features, sub, e -> requireNonNull(e.jvm).stream());
            List<String> progArgs = collectArgs(features, sub, e -> requireNonNull(e.game).stream());

            List<String> command = new ArrayList<>(jvmArgs.size() + progArgs.size() + 2);
            command.add(javaExecutable.toAbsolutePath().toString());
            command.addAll(jvmArgs);
            command.add(mainClass);
            command.addAll(progArgs);
            return new ProcessBuilder()
                    .directory(gameDir.toFile())
                    .command(command);
        } catch (Throwable ex) {
            throw new LaunchException("Failed to prepare instance '" + instance.getName() + "'(" + instance.getUuid() + ").", ex);
        }
    }

    private void prepareManifests(Path versionsDir) throws IOException {
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
        List<Task<?>> tasks = InstallAssetsTask.build(requireNonNull(manifest.assetIndex, "First Version Manifest missing AssetIndex. This should not happen."));
        for (Task<?> task : tasks) {
            try {
                task.execute();
            } catch (Throwable ex) {
                throw new IOException("Failed to execute asset update task.", ex);
            }
        }
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

    private List<String> collectArgs(Set<String> features, StrSubstitutor sub, Function<VersionManifest.Arguments, Stream<VersionManifest.EvalValue>> func) {
        return manifests.stream()
                .flatMap(e -> func.apply(e.arguments))
                .flatMap(e -> e.eval(features).stream())
                .map(sub::replace)
                .collect(Collectors.toList());
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
        classpath.add(versionsDir.resolve(instance.modLoader).resolve(instance.modLoader + ".jar"));
        return classpath;
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

    /**
     * Thrown when the instance is unable to be launched for a specific reason.
     */
    public static class LaunchException extends Exception {

        public LaunchException(String message) {
            super(message);
        }

        public LaunchException(String message, Throwable t) {
            super(message, t);
        }
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
}
