package net.creeperhost.creeperlauncher.install.tasks.modloader;

import net.creeperhost.creeperlauncher.install.tasks.NewDownloadTask;
import net.creeperhost.creeperlauncher.install.tasks.Task;
import net.creeperhost.creeperlauncher.install.tasks.modloader.forge.AbstractForgeInstallTask;
import net.creeperhost.creeperlauncher.minecraft.jsons.VersionListManifest;
import net.creeperhost.creeperlauncher.minecraft.jsons.VersionManifest;
import net.creeperhost.creeperlauncher.pack.LocalInstance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by covers1624 on 28/1/22.
 */
public abstract class ModLoaderInstallTask implements Task<String> {

    private static final Logger LOGGER = LogManager.getLogger();

    /*
     Forge downloads:
     0               - 1.3.2-4.0.0.182  - *-client.zip
     1.3.2-4.0.0.183 - 1.6.1-8.9.0.751  - *-universal.zip
     1.5.2-7.8.0.684 - current          - *-installer.jar

     Forge started generating universal jars at the same time as installer jars there are a
     few specific Forge versions captured by this range (very early 1.16.1)
     which do not contain installers, these should fall back to universal.zip and requiring
     an external manifest as they don't have a universal jar.
    */

    // See https://maven.apache.org/enforcer/enforcer-rules/versionRanges.html

    // Min and Max selectors for when Forge switched their formats as per the above table.
    public static final VersionRange FORGE_CLIENT_ZIP = createRange("(,4.0.0.182]");
    public static final VersionRange FORGE_UNIVERSAL_ZIP = createRange("[4.0.0.183,8.9.0.751]");
    public static final VersionRange FORGE_INSTALLER_JAR = createRange("[7.8.0.684,)");

    // The minecraft version range which denotes a 'jar mod' environment.
    public static final VersionRange FORGE_LEGACY_INSTALL = createRange("[1.2,1.5.2]");

    public static ModLoaderInstallTask createInstallTask(LocalInstance instance, String mcVersion, String mlName, String mlVersion) throws IOException {
        return switch (mlName) {
            case "forge" -> AbstractForgeInstallTask.createInstallTask(instance, mcVersion, mlVersion);
            case "fabric" -> new FabricInstallTask(mcVersion, mlVersion);
            default -> throw new IllegalArgumentException("Unknown ModLoader name: " + mlName);
        };
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

    private static VersionRange createRange(String spec) {
        try {
            return VersionRange.createFromVersionSpec(spec);
        } catch (InvalidVersionSpecificationException e) {
            throw new RuntimeException(e);
        }
    }
}
