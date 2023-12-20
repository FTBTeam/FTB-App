package net.creeperhost.creeperlauncher.install.tasks.modloader;

import net.covers1624.quack.gson.JsonUtils;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.install.ProgressTracker;
import net.creeperhost.creeperlauncher.install.tasks.*;
import net.creeperhost.creeperlauncher.minecraft.jsons.VersionManifest;
import net.creeperhost.creeperlauncher.util.CancellationToken;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by covers1624 on 28/1/22.
 */
public abstract class FabricInstallTask extends ModLoaderInstallTask {

    private static final String FABRIC_META = "https://meta.fabricmc.net/";
    private static final String QUILT_META = "https://meta.quiltmc.org/";

    private final String mcVersion;
    private final String loaderVersion;

    private final String versionName;

    private FabricInstallTask(CancellationToken cancelToken, ProgressTracker tracker, String loaderName, String mcVersion, String loaderVersion) {
        super(cancelToken, tracker);
        this.mcVersion = mcVersion;
        this.loaderVersion = loaderVersion;

        versionName = loaderName + "-" + mcVersion + "-" + loaderVersion;
    }

    public static FabricInstallTask fabric(CancellationToken cancelToken, ProgressTracker tracker, String mcVersion, String loaderVersion) {
        return new FabricInstallTask(cancelToken, tracker, "fabric-loader", mcVersion, loaderVersion) {
            @Override
            protected DownloadTask getProfileDownload(Path dest) {
                return DownloadTask.builder()
                        .url(FABRIC_META + "v2/versions/loader/" + mcVersion + "/" + loaderVersion + "/profile/json")
                        .dest(dest)
                        .build();
            }
        };
    }

    public static FabricInstallTask quilt(CancellationToken cancelToken, ProgressTracker tracker, String mcVersion, String loaderVersion) {
        return new FabricInstallTask(cancelToken, tracker, "quilt-loader", mcVersion, loaderVersion) {
            @Override
            protected DownloadTask getProfileDownload(Path dest) {
                return DownloadTask.builder()
                        .url(QUILT_META + "v3/versions/loader/" + mcVersion + "/" + loaderVersion + "/profile/json")
                        .dest(dest)
                        .build();
            }
        };
    }

    @Override
    public void execute() throws Throwable {
        Path versionsDir = Constants.BIN_LOCATION.resolve("versions");
        Path librariesDir = Constants.BIN_LOCATION.resolve("libraries");

        Path versionJson = versionsDir.resolve(versionName).resolve(versionName + ".json");

        tracker.setCustomStatus("Download manifest");
        VersionManifest loaderManifest = downloadLoaderManifest(versionJson, cancelToken);
        tracker.setCustomStatus("Download vanilla Minecraft");
        downloadVanilla(versionsDir, mcVersion, cancelToken, tracker.dynamicListener());

        tracker.setCustomStatus("Download libraries");
        long totalSize = 0;
        List<Task> libDownloads = new ArrayList<>();
        for (VersionManifest.Library library : loaderManifest.libraries) {
            DownloadTask task = library.createDownloadTask(librariesDir, true);

            if (task == null) throw new IOException("Unable to download or locate library: " + library.name);
            if (task.isRedundant()) continue;

            totalSize += task.getSizeEstimate();
            libDownloads.add(task.wrapStepComplete(tracker));
        }
        if (libDownloads.isEmpty()) return;

        tracker.setDynamicStepCount(libDownloads.size());
        TaskProgressAggregator.aggregate(tracker.dynamicListener(), totalSize, l -> {
            ParallelTaskHelper.executeInParallel(cancelToken, Task.TASK_POOL, libDownloads, l);
        });
    }

    @Override
    public String getModLoaderTarget() {
        return versionName;
    }

    protected abstract DownloadTask getProfileDownload(Path dest);

    private VersionManifest downloadLoaderManifest(Path dest, @Nullable CancellationToken cancellationToken) throws IOException {
        DownloadTask task = getProfileDownload(dest);
        if (!task.isRedundant()) {
            task.execute(cancellationToken, TaskProgressListener.NOP);
        }

        return JsonUtils.parse(VersionManifest.GSON, dest, VersionManifest.class);
    }
}
