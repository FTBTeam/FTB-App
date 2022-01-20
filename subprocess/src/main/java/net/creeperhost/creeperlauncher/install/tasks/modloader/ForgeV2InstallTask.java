package net.creeperhost.creeperlauncher.install.tasks.modloader;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import net.covers1624.jdkutils.JavaInstall;
import net.covers1624.jdkutils.JavaVersion;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.io.IOUtils;
import net.covers1624.quack.maven.MavenNotation;
import net.covers1624.quack.util.HashUtils;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.data.installerv2.InstallManifest;
import net.creeperhost.creeperlauncher.install.tasks.NewDownloadTask;
import net.creeperhost.creeperlauncher.install.tasks.Task;
import net.creeperhost.creeperlauncher.install.tasks.TaskProgressListener;
import net.creeperhost.creeperlauncher.minecraft.jsons.VersionListManifest;
import net.creeperhost.creeperlauncher.minecraft.jsons.VersionManifest;
import net.creeperhost.creeperlauncher.pack.CancellationToken;
import net.creeperhost.creeperlauncher.util.StreamGobblerLog;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * Created by covers1624 on 14/1/22.
 */
@SuppressWarnings ("UnstableApiUsage")
public class ForgeV2InstallTask implements Task<Void> {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Path installerJar;

    public ForgeV2InstallTask(Path installerJar) {
        this.installerJar = installerJar;
    }

    public static void main(String[] args) throws Throwable {
        System.setProperty("ftba.dataDirOverride", "./");
        new ForgeV2InstallTask(Path.of("forge-1.16.5-36.2.23-installer.jar")).execute(null, null);
    }

    @Override
    public void execute(@Nullable CancellationToken cancelToken, @Nullable TaskProgressListener listener) throws Throwable {
        // Things this task needs to do:
        // - Grab the Minecraft VersionManifest for the version of forge we are installing.
        // - Grab JRE for the targeted minecraft version. (for executing tasks with)
        // - Download Forge Installer from maven
        // - Extract (zipfs open) and parse manifest.
        // - ....

        Path versionsDir = Constants.BIN_LOCATION.resolve("versions");
        Path librariesDir = Constants.BIN_LOCATION.resolve("libraries");

        try (FileSystem fs = IOUtils.getJarFileSystem(installerJar, true)) {
            Path installerRoot = fs.getPath("/");
            // Parse the installer manifest.
            InstallManifest instManifest = JsonUtils.parse(InstallManifest.GSON, installerRoot.resolve("install_profile.json"), InstallManifest.class);
            VersionManifest forgeManifest = JsonUtils.parse(VersionManifest.GSON, installerRoot.resolve(instManifest.json), VersionManifest.class);

            // Install the Version Manifest.
            Files.copy(
                    installerRoot.resolve(instManifest.json),
                    IOUtils.makeParents(versionsDir.resolve(instManifest.version).resolve(instManifest.version + ".json")),
                    StandardCopyOption.REPLACE_EXISTING
            );
            VersionManifest vanillaManifest = downloadVanilla(versionsDir, instManifest.minecraft);

            List<VersionManifest.Library> libraries = new LinkedList<>();
            libraries.addAll(forgeManifest.libraries);
            libraries.addAll(instManifest.libraries);

            for (VersionManifest.Library library : libraries) {
                processLibrary(cancelToken, installerRoot, librariesDir, library);
            }

            runProcessors(instManifest, vanillaManifest, installerRoot, librariesDir, versionsDir);
        }
    }

    @Override
    public @Nullable Void getResult() {
        return null;
    }

    private static VersionManifest downloadVanilla(Path versionsDir, String version) throws IOException {
        VersionListManifest listManifest = VersionListManifest.update(versionsDir);
        VersionManifest manifest = listManifest.resolve(versionsDir, version);
        if (manifest == null) {
            LOGGER.error("No vanilla version manifest found for {}", version);
            throw new IOException("No vanilla version manifest found for " + version);
        }

        NewDownloadTask clientDownload = manifest.getClientDownload(versionsDir);
        if (clientDownload == null) {
            LOGGER.warn("Failed to find 'client' download for {}. Skipping..", version);
            return manifest;
        }

        if (!clientDownload.isRedundant()) {
            clientDownload.execute(null, null);
        }
        return manifest;
    }

    private static void processLibrary(CancellationToken token, Path installerRoot, Path librariesDir, VersionManifest.Library library) throws IOException {
        NewDownloadTask downloadTask = library.createDownloadTask(librariesDir);

        Path installerMavenFile = library.name.toPath(installerRoot.resolve("maven"));
        Path dest = downloadTask != null ? downloadTask.getDest() : library.name.toPath(librariesDir);

        if (downloadTask != null && downloadTask.isRedundant()) {
            return;
        }

        if (Files.exists(installerMavenFile)) {
            LOGGER.info("File exists inside installer: {}", library.name);
            Files.copy(installerMavenFile, IOUtils.makeParents(dest), StandardCopyOption.REPLACE_EXISTING);
            if (downloadTask != null && !downloadTask.isRedundant()) {
                LOGGER.error("Failed to validate file. TODO, better message.");
                // TODO better logging for why this failed.
                //  Perhaps a better way to do this would be to weave these local 'Search paths' through to the DownloadTask and let it handle it
                //  as we already have code in there for the LocalCache.
            }
            return;
        }

        if (downloadTask == null) {
            LOGGER.info("Skipping download of '{}' file does not exist in installer, and is not downloadable.", library.name);
            return;
        }
        downloadTask.execute(token, null); // TODO progress
    }

    private void runProcessors(InstallManifest manifest, VersionManifest vanillaManifest, Path installerRoot, Path librariesDir, Path versionsDir) throws IOException {
        Path javaHome = Constants.JDK_INSTALL_MANAGER.provisionJdk(vanillaManifest.getJavaVersionOrDefault(JavaVersion.JAVA_1_8), true, null);
        Path javaExecutable = JavaInstall.getJavaExecutable(javaHome, true);

        Path tempDir = Files.createTempDirectory("forge_installer_");
        tempDir.toFile().deleteOnExit();
        Map<String, String> data = new HashMap<>();
        for (Map.Entry<String, InstallManifest.DataEntry> entry : manifest.data.entrySet()) {
            String value = entry.getValue().client;
            if (surroundedBy(value, '[', ']')) { // Artifact
                value = MavenNotation.parse(topAndTail(value)).toPath(librariesDir).toAbsolutePath().toString();
            } else if (surroundedBy(value, '\'', '\'')) { // Literal
                value = topAndTail(value);
            } else {
                Path extracted = tempDir.resolve(StringUtils.removeStart(value, "/"));
                Files.copy(installerRoot.resolve(value), IOUtils.makeParents(extracted), StandardCopyOption.REPLACE_EXISTING);
                value = extracted.toAbsolutePath().toString();
            }
            data.put(entry.getKey(), value); // Hard coded to client.
        }

        data.put("SIDE", "client");
        data.put("MINECRAFT_JAR", versionsDir.resolve(vanillaManifest.id).resolve(vanillaManifest.id + ".jar").toAbsolutePath().toString());
        data.put("MINECRAFT_VERSION", vanillaManifest.id);
        data.put("ROOT", Constants.BIN_LOCATION.toAbsolutePath().toString());
        data.put("INSTALLER", installerJar.toAbsolutePath().toString());
        data.put("LIBRARY_DIR", librariesDir.toAbsolutePath().toString());

        for (InstallManifest.Processor processor : manifest.processors) {
            runProcessor(processor, data, javaExecutable, librariesDir);
        }
    }

    private void runProcessor(InstallManifest.Processor processor, Map<String, String> data, Path javaExecutable, Path librariesDir) throws IOException {
        Map<Path, String> outputs = new HashMap<>();

        boolean cached = !processor.outputs.isEmpty();
        for (Map.Entry<String, String> entry : processor.outputs.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (surroundedBy(key, '[', ']')) {
                key = MavenNotation.parse(topAndTail(key)).toPath(librariesDir).toAbsolutePath().toString();
            } else {
                key = replaceTokens(data, key);
            }

            if (value != null) {
                value = replaceTokens(data, value);
            }

            Path output = Path.of(key);
            outputs.put(output, value);

            if (Files.notExists(output)) {
                cached = false;
                continue;
            }
            HashCode hash = HashUtils.hash(Hashing.sha1(), output);
            if (!HashUtils.equals(hash, value)) {
                LOGGER.warn("Output '{}' failed to validate, it will be deleted.", output);
                LOGGER.warn(" Expected: {}", value);
                LOGGER.warn(" Got     : {}", hash);
                Files.delete(output);
                cached = false;
            } else {
                LOGGER.info("Output '{}' Validated: {}", output, hash);
            }
        }
        if (cached) {
            LOGGER.info("Cache validated, Skipping processor.");
            return;
        }

        Path jar = processor.jar.toPath(librariesDir);
        List<Path> classpath = new ArrayList<>(processor.classpath.size() + 2);
        classpath.add(jar);
        for (MavenNotation mavenNotation : processor.classpath) {
            Path path = mavenNotation.toPath(librariesDir);
            classpath.add(path);
        }
        // Theoretically not needed, but mimics the classloading structure of the Forge installer properly
        classpath.add(installerJar);
        for (Path path : classpath) {
            if (!Files.exists(path)) {
                LOGGER.error("Processor classpath entry does not exist: {}", path);
                throw new IOException("Processor classpath entry does not exist: " + path);
            }
        }

        List<String> command = new ArrayList<>(5 + processor.args.size());
        command.add(javaExecutable.toAbsolutePath().toString());
        command.add("-cp");
        command.add(classpath.stream().map(e -> e.toAbsolutePath().toString()).collect(Collectors.joining(File.pathSeparator)));
        command.add(getMainClass(jar));
        command.add(jar.toAbsolutePath().toString());

        for (String arg : processor.args) {
            if (surroundedBy(arg, '[', ']')) {
                command.add(MavenNotation.parse(topAndTail(arg)).toPath(librariesDir).toAbsolutePath().toString());
            } else {
                command.add(replaceTokens(data, arg));
            }
        }

        ProcessBuilder builder = new ProcessBuilder()
                .directory(Constants.BIN_LOCATION.toFile())
                .command(command);
        LOGGER.info("Starting processor with command '{}'", String.join(" ", builder.command()));
        try {
            Process process = builder.start();
            CompletableFuture<Void> stdoutFuture = StreamGobblerLog.redirectToLogger(process.getInputStream(), LOGGER::info);
            CompletableFuture<Void> stderrFuture = StreamGobblerLog.redirectToLogger(process.getErrorStream(), LOGGER::error);

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
            if (exit != 0) {
                LOGGER.error("Processor process exited with non zero exit status. {}", exit);
                throw new IOException("Processor process exited with non zero exit status code. " + exit);
            }
        } catch (IOException ex) {
            LOGGER.error("Failed to run processor.", ex);
            throw ex;
        }

        boolean validated = true;
        for (Map.Entry<Path, String> entry : outputs.entrySet()) {
            Path output = entry.getKey();
            String value = entry.getValue();
            if (Files.notExists(output)) {
                LOGGER.error("Output '{}' does not exist.", output);
                validated = false;
            } else {
                HashCode hash = HashUtils.hash(Hashing.sha1(), output);
                if (!HashUtils.equals(hash, value)) {
                    LOGGER.warn("Output '{}' failed to validate.", output);
                    LOGGER.warn(" Expected: {}", value);
                    LOGGER.warn(" Got     : {}", hash);
                    validated = false;
                } else {
                    LOGGER.info("Output '{}' Validated: {}", output, hash);
                }
            }
        }
        if (!validated) {
            LOGGER.error("Processor output validation errors occurred.");
            throw new IOException("Processor output validation errors occurred.");
        }
    }

    private static String topAndTail(String s) {
        return s.substring(1, s.length() - 1);
    }

    private static boolean surroundedBy(String s, char start, char end) {
        return s.charAt(0) == start && s.charAt(s.length() - 1) == end;
    }

    private static String getMainClass(Path jar) throws IOException {
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            return jarFile.getManifest().getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
        }
    }

    // This function is borrowed from the MinecraftForge Installer and has been
    // entirely unmodified, except for non-functional formatting.
    private static String replaceTokens(Map<String, String> tokens, String value) {
        StringBuilder buf = new StringBuilder();

        for (int x = 0; x < value.length(); x++) {
            char c = value.charAt(x);
            if (c == '\\') {
                if (x == value.length() - 1) {
                    throw new IllegalArgumentException("Illegal pattern (Bad escape): " + value);
                }
                buf.append(value.charAt(++x));
            } else if (c == '{' || c == '\'') {
                StringBuilder key = new StringBuilder();
                for (int y = x + 1; y <= value.length(); y++) {
                    if (y == value.length()) {
                        throw new IllegalArgumentException("Illegal pattern (Unclosed " + c + "): " + value);
                    }
                    char d = value.charAt(y);
                    if (d == '\\') {
                        if (y == value.length() - 1) {
                            throw new IllegalArgumentException("Illegal pattern (Bad escape): " + value);
                        }
                        key.append(value.charAt(++y));
                    } else if (c == '{' && d == '}') {
                        x = y;
                        break;
                    } else if (c == '\'' && d == '\'') {
                        x = y;
                        break;
                    } else {
                        key.append(d);
                    }
                }
                if (c == '\'') {
                    buf.append(key);
                } else {
                    if (!tokens.containsKey(key.toString())) {
                        throw new IllegalArgumentException("Illegal pattern: " + value + " Missing Key: " + key);
                    }
                    buf.append(tokens.get(key.toString()));
                }
            } else {
                buf.append(c);
            }
        }

        return buf.toString();
    }
}
