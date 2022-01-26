package net.creeperhost.creeperlauncher.install.tasks.modloader;

import net.creeperhost.creeperlauncher.install.FileValidation;
import net.creeperhost.creeperlauncher.install.tasks.NewDownloadTask;
import net.creeperhost.creeperlauncher.minecraft.jsons.VersionListManifest;
import net.creeperhost.creeperlauncher.minecraft.jsons.VersionManifest;
import net.creeperhost.creeperlauncher.pack.CancellationToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

/**
 * Created by covers1624 on 25/1/22.
 */
public class AbstractForgeInstallTask {

    private static final Logger LOGGER = LogManager.getLogger();

    protected Path processLibrary(@Nullable CancellationToken token, Path installerRoot, Path librariesDir, VersionManifest.Library library) throws IOException {
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

    protected static VersionManifest downloadVanilla(Path versionsDir, String version) throws IOException {
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
