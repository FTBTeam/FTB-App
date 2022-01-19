package net.creeperhost.creeperlauncher.install.tasks.modloader;

import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.io.IOUtils;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.data.installerv2.InstallManifest;
import net.creeperhost.creeperlauncher.install.tasks.NewDownloadTask;
import net.creeperhost.creeperlauncher.install.tasks.Task;
import net.creeperhost.creeperlauncher.install.tasks.TaskProgressListener;
import net.creeperhost.creeperlauncher.minecraft.jsons.VersionListManifest;
import net.creeperhost.creeperlauncher.minecraft.jsons.VersionManifest;
import net.creeperhost.creeperlauncher.pack.CancellationToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by covers1624 on 14/1/22.
 */
public class ForgeV2InstallTask implements Task<Void> {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Path installerJar;

    public ForgeV2InstallTask(Path installerJar) {
        this.installerJar = installerJar;
    }

    public static void main(String[] args) throws Throwable {
        System.setProperty("ftba.dataDirOverride", "./");
        new ForgeV2InstallTask(Path.of("forge-1.12.2-14.23.5.2860-installer.jar")).execute(null, null);
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
}
