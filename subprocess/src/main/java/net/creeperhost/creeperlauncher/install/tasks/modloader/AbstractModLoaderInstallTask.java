package net.creeperhost.creeperlauncher.install.tasks.modloader;

import net.creeperhost.creeperlauncher.install.tasks.NewDownloadTask;
import net.creeperhost.creeperlauncher.install.tasks.Task;
import net.creeperhost.creeperlauncher.minecraft.jsons.VersionListManifest;
import net.creeperhost.creeperlauncher.minecraft.jsons.VersionManifest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by covers1624 on 28/1/22.
 */
public abstract class AbstractModLoaderInstallTask implements Task<String> {

    private static final Logger LOGGER = LogManager.getLogger();

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
}
