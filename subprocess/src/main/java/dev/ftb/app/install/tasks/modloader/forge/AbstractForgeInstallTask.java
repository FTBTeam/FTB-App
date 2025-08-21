package dev.ftb.app.install.tasks.modloader.forge;

import com.google.gson.JsonObject;
import dev.ftb.app.AppMain;
import dev.ftb.app.Constants;
import dev.ftb.app.data.forge.VersionOverrides;
import dev.ftb.app.data.forge.installerv1.InstallProfile;
import dev.ftb.app.install.FileValidation;
import dev.ftb.app.install.tasks.DownloadTask;
import dev.ftb.app.install.tasks.modloader.ModLoaderInstallTask;
import dev.ftb.app.minecraft.jsons.VersionManifest;
import dev.ftb.app.pack.CancellationToken;
import dev.ftb.app.pack.Instance;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.io.IOUtils;
import net.covers1624.quack.maven.MavenNotation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import static dev.ftb.app.install.tasks.DownloadTask.*;
import static org.apache.commons.lang3.StringUtils.appendIfMissing;

public abstract class AbstractForgeInstallTask extends ModLoaderInstallTask {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final MavenNotation FORGE_NOTATION = MavenNotation.parse("net.minecraftforge:forge");
    private static final MavenNotation NEO_FORGE_NOTATION = MavenNotation.parse("net.neoforged:forge");
    private static final MavenNotation NEO_FORGE_1_20_2_PLUS_NOTATION = MavenNotation.parse("net.neoforged:neoforge");

    private static final VersionOverrides SPECIAL_VERSIONS = VersionOverrides.compute();

    @Nullable
    protected String versionName;

    @Override
    public final String getModLoaderTarget() {
        return Objects.requireNonNull(versionName);
    }

    public static AbstractForgeInstallTask createInstallTask(Instance instance, String mcVersion, String forgeVersion) throws IOException {
        if (FORGE_LEGACY_INSTALL.containsVersion(new DefaultArtifactVersion(mcVersion))) {
            return new LegacyForgeInstallTask(instance, mcVersion, forgeVersion);
        }
        MavenNotation notation = getForgeNotation(mcVersion, forgeVersion);
        if (!"universal".equals(notation.classifier) && !"jar".equals(notation.extension)) {
            throw new IllegalStateException("Expected Forge 'universal.jar'. Got: " + notation);
        }
        notation = notation.withClassifier("installer");

        // TODO, can we maybe merge both install tasks and do this detection in there?
        //       I dislike that we have to download this file in a call path that just evaluates
        //       what needs to be done for an installation.
        DownloadTask task = builder()
                .url(appendIfMissing(Constants.CH_MAVEN, notation.toPath()))
                .dest(notation.toPath(AppMain.paths().mcLibrariesDir()))
                .withValidation(DownloadValidation.of().withUseETag(true).withUseOnlyIfModified(true))
                .tryCompanionHashes()
                .build();

        task.execute(null, null);
        return detectInstallerVersion(instance, task.getDest());
    }

    public static AbstractForgeInstallTask createNeoForgeInstallTask(Instance instance, String mcVersion, String neoForgeVersion) throws IOException {
        MavenNotation versionSpecificNotation = NEO_FORGE_1_20_2_PLUS_NOTATION.withVersion(neoForgeVersion);

        if (mcVersion.equals("1.20.1")) {
            versionSpecificNotation = NEO_FORGE_NOTATION.withVersion(mcVersion + "-" + neoForgeVersion);
        }

        MavenNotation notation = versionSpecificNotation
                .withClassifier("installer");

        // TODO, this is a common path with above.
        DownloadTask task = builder()
                .url(appendIfMissing(Constants.CH_MAVEN, notation.toPath()))
                .dest(notation.toPath(AppMain.paths().mcLibrariesDir()))
                .withValidation(DownloadValidation.of().withUseETag(true).withUseOnlyIfModified(true))
                .tryCompanionHashes()
                .build();

        task.execute(null, null);
        return new ForgeV2InstallTask(instance, task.getDest());
    }

    private static AbstractForgeInstallTask detectInstallerVersion(Instance instance, Path installer) throws IOException {
        try (FileSystem fs = IOUtils.getJarFileSystem(installer, true)) {
            Path installProfile = fs.getPath("/install_profile.json");

            // Forge may have changed their format?
            if (Files.notExists(installProfile)) throw new IOException("Failed to find 'install_profile.json' in Forge installer.");

            JsonObject json = JsonUtils.parse(InstallProfile.GSON, installProfile, JsonObject.class);
            if (json.has("spec")) {
                return new ForgeV2InstallTask(instance, installer);
            }
            return new ForgeV1InstallTask(instance, installer);
        }
    }

    protected static MavenNotation getForgeNotation(String mcVersion, String forgeVersion) {
        ArtifactVersion forgeVers = new DefaultArtifactVersion(forgeVersion);

        MavenNotation notation = getVersionNotation(mcVersion, forgeVersion);
        if (FORGE_CLIENT_ZIP.containsVersion(forgeVers)) {
            return notation.withClassifier("client")
                    .withExtension("zip");
        }

        if (FORGE_UNIVERSAL_ZIP.containsVersion(forgeVers)) {
            return notation.withClassifier("universal")
                    .withExtension("zip");
        }
        if (FORGE_INSTALLER_JAR.containsVersion(forgeVers)) {
            return notation.withClassifier("universal")
                    .withExtension("jar");
        }

        throw new UnsupportedOperationException("Unable to create notation for Forge version. Minecraft Version: " + mcVersion + ", Forge Version: " + forgeVersion);
    }

    protected static MavenNotation getForgeInstallerNotation(String mcVersion, String forgeVersion) {
        if (!FORGE_INSTALLER_JAR.containsVersion(new DefaultArtifactVersion(forgeVersion))) {
            throw new UnsupportedOperationException("Forge version '" + forgeVersion + "' is not known to have an installer.");
        }
        return getVersionNotation(mcVersion, forgeVersion).withClassifier("installer").withClassifier("jar");
    }

    private static MavenNotation getVersionNotation(String mcVersion, String forgeVersion) {
        String override = SPECIAL_VERSIONS.find(forgeVersion);
        if (override != null) {
            LOGGER.info("Overriding Forge version from {} to {}.", forgeVersion, override);
            return FORGE_NOTATION.withVersion(override);
        }

        return FORGE_NOTATION.withVersion(mcVersion + "-" + forgeVersion);
    }

    protected static @Nullable Path processLibrary(@Nullable CancellationToken token, Path installerRoot, Path librariesDir, VersionManifest.Library library) throws IOException {
        LOGGER.info("Processing library " + library.name);
        Path installerMavenFile = library.name.toPath(installerRoot.resolve("maven"));
        if (!library.hasDownloadPath() && Files.notExists(installerMavenFile)) {
            // 1.20.4+ forge adds a library with a null URL, which is not located inside the jar.
            // All previous versions had this file inside the installer jar, instead this file is installed
            // by the processors to the correct place in the libraries dir. Just ignore these.
            // File has no download path and is not inside the installer jar. This must be a generated artifact.
            LOGGER.info(" Library does not exist inside forge installer jar. Must be a generated artifact.");
            return null;
        }
        DownloadTask downloadTask = library.createDownloadTask(librariesDir, false);
        if (downloadTask == null) throw new IOException("Unable to download or locate library: " + library.name);

        downloadTask = downloadTask.toBuilder()
                .withFileLocator(new PackedJarLocator(installerMavenFile))
                .build();

        if (!downloadTask.isRedundant()) {
            downloadTask.execute(token, null); // TODO progress
        }
        return downloadTask.getDest();

    }

    protected static String topAndTail(String s) {
        return s.substring(1, s.length() - 1);
    }

    protected static boolean surroundedBy(String s, char start, char end) {
        return s.charAt(0) == start && s.charAt(s.length() - 1) == end;
    }

    protected static String getMainClass(Path jar) throws IOException {
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            return jarFile.getManifest().getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
        }
    }

    // This function is borrowed from the MinecraftForge Installer and has been
    // entirely unmodified, except for non-functional formatting.
    protected static String replaceTokens(Map<String, String> tokens, String value) {
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

    protected record PackedJarLocator(Path localPath) implements LocalFileLocator {

        @Override
        public Path getLocalFile(FileValidation validation, Path dest) {
            return localPath;
        }

        @Override
        public void onFileDownloaded(FileValidation validation, Path dest) {
        }
    }
}
