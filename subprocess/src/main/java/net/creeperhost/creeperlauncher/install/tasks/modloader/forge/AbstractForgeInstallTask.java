package net.creeperhost.creeperlauncher.install.tasks.modloader.forge;

import net.covers1624.quack.maven.MavenNotation;
import net.creeperhost.creeperlauncher.install.FileValidation;
import net.creeperhost.creeperlauncher.install.tasks.NewDownloadTask;
import net.creeperhost.creeperlauncher.install.tasks.modloader.ModLoaderInstallTask;
import net.creeperhost.creeperlauncher.minecraft.jsons.VersionManifest;
import net.creeperhost.creeperlauncher.pack.CancellationToken;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

/**
 * Created by covers1624 on 25/1/22.
 */
public abstract class AbstractForgeInstallTask extends ModLoaderInstallTask {

    private static final MavenNotation FORGE_NOTATION = MavenNotation.parse("net.minecraftforge:forge");

    @Nullable
    protected String versionName;

    @Nullable
    @Override
    public final String getResult() {
        return versionName;
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
        return FORGE_NOTATION.withVersion(mcVersion + "-" + forgeVersion);
    }

    protected static Path processLibrary(@Nullable CancellationToken token, Path installerRoot, Path librariesDir, VersionManifest.Library library) throws IOException {
        NewDownloadTask downloadTask = library.createDownloadTask(librariesDir, false);
        if (downloadTask == null) throw new IOException("Unable to download or locate library: " + library.name);

        Path installerMavenFile = library.name.toPath(installerRoot.resolve("maven"));

        downloadTask = downloadTask.toBuilder()
                .wFileLocator(new PackedJarLocator(installerMavenFile))
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

    protected record PackedJarLocator(Path localPath) implements NewDownloadTask.LocalFileLocator {

        @Override
        public Path getLocalFile(String url, FileValidation validation, Path dest) {
            return localPath;
        }

        @Override
        public void onFileDownloaded(String url, FileValidation validation, Path dest) {
        }
    }
}
