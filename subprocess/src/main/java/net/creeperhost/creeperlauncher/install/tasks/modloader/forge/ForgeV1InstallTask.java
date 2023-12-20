package net.creeperhost.creeperlauncher.install.tasks.modloader.forge;

import com.google.gson.JsonObject;
import net.covers1624.quack.collection.ColUtils;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.io.IOUtils;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.data.forge.installerv1.InstallProfile;
import net.creeperhost.creeperlauncher.install.OperationProgressTracker;
import net.creeperhost.creeperlauncher.install.ProgressTracker;
import net.creeperhost.creeperlauncher.install.tasks.*;
import net.creeperhost.creeperlauncher.minecraft.jsons.VersionManifest;
import net.creeperhost.creeperlauncher.util.CancellationToken;
import net.creeperhost.creeperlauncher.pack.Instance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Handles the installation of a Forge V1 installer.
 * <p>
 * Unlike V2, this can pretty much just extract the universal jar and install the profile.
 * However, we make a good effort to install all libraries and necessary files during the installation process here.
 * <p>
 * Created by covers1624 on 24/1/22.
 */
public class ForgeV1InstallTask extends AbstractForgeInstallTask {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Instance instance;
    private final Path installerJar;

    public ForgeV1InstallTask(CancellationToken cancelToken, ProgressTracker tracker, Instance instance, Path installerJar) {
        super(cancelToken, tracker);
        this.instance = instance;
        this.installerJar = installerJar;
    }

    @Override
    public void execute() throws Throwable {
        Path versionsDir = Constants.BIN_LOCATION.resolve("versions");
        Path librariesDir = Constants.BIN_LOCATION.resolve("libraries");

        try (FileSystem fs = IOUtils.getJarFileSystem(installerJar, true)) {
            Path installerRoot = fs.getPath("/");
            InstallProfile profile = JsonUtils.parse(InstallProfile.GSON, installerRoot.resolve("install_profile.json"), InstallProfile.class);
            if (profile.install.transform != null) throw new IOException("Unable to process Forge v1 Installer with transforms.");

            versionName = profile.install.target;

            cancelToken.throwIfCancelled();

            tracker.setCustomStatus("Download vanilla Minecraft");
            VersionManifest vanillaManifest = downloadVanilla(versionsDir, profile.install.minecraft, cancelToken, tracker.dynamicListener());
            if (profile.versionInfo.inheritsFrom == null || profile.versionInfo.jar == null) {
                Path srcJar = versionsDir.resolve(vanillaManifest.id).resolve(vanillaManifest.id + ".jar");
                Path destJar = IOUtils.makeParents(versionsDir.resolve(versionName).resolve(versionName + ".jar"));
                if (profile.install.stripMeta) {
                    stripMeta(srcJar, destJar);
                } else {
                    Files.copy(
                            srcJar,
                            destJar,
                            StandardCopyOption.REPLACE_EXISTING
                    );
                }
            }

            tracker.setCustomStatus("Download libraries");
            long totalSize = 0;
            List<Task> libraryTasks = new ArrayList<>();
            for (InstallProfile.Library library : profile.versionInfo.libraries) {
                if (library.clientreq == null || !library.clientreq) continue; // Skip, mirrors forge logic.
                DownloadTask task = prepareDownloadTask(installerRoot, librariesDir, library);
                if (!task.isRedundant()) {
                    totalSize += task.getSizeEstimate();
                    libraryTasks.add(task.wrapStepComplete(tracker));
                }
            }

            if (!libraryTasks.isEmpty()) {
                tracker.setDynamicStepCount(libraryTasks.size());
                TaskProgressAggregator.aggregate(tracker.dynamicListener(), totalSize, l -> {
                    ParallelTaskHelper.executeInParallel(cancelToken, Task.TASK_POOL, libraryTasks, l);
                });
            }

            cancelToken.throwIfCancelled();

            tracker.setCustomStatus("Setup launch profile.");
            LOGGER.info("Extracting {} from installer jar.", profile.install.path);
            Path universalPath = profile.install.path.toPath(librariesDir);
            Files.copy(installerRoot.resolve(profile.install.filePath), IOUtils.makeParents(universalPath), StandardCopyOption.REPLACE_EXISTING);

            JsonObject profileJson = JsonUtils.parse(InstallProfile.GSON, installerRoot.resolve("install_profile.json"), JsonObject.class);
            LOGGER.info("Writing version profile {}.", versionName);
            Path versionJson = versionsDir.resolve(versionName).resolve(versionName + ".json");
            JsonUtils.write(InstallProfile.GSON, IOUtils.makeParents(versionJson), profileJson.get("versionInfo"));

            ForgeLegacyLibraryHelper.installLegacyLibs(cancelToken, tracker, instance, profile.install.minecraft);
        }
    }

    private void stripMeta(Path input, Path output) throws IOException {
        try (ZipFile zIn = new ZipFile(input.toFile());
             ZipOutputStream zOut = new ZipOutputStream(Files.newOutputStream(output))) {
            for (ZipEntry entry : ColUtils.iterable(zIn.entries())) {
                if (entry.getName().startsWith("META-INF")) continue;
                if (entry.isDirectory()) {
                    zOut.putNextEntry(entry);
                } else {
                    ZipEntry newEntry = new ZipEntry(entry.getName());
                    newEntry.setTime(entry.getTime());
                    zOut.putNextEntry(newEntry);
                    IOUtils.copy(zIn.getInputStream(entry), zOut);
                }
            }
        }
    }
}
