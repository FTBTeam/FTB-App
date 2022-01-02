package net.creeperhost.creeperlauncher.install.tasks;

import com.google.common.hash.Hashing;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.install.tasks.NewDownloadTask.DownloadValidation;
import net.creeperhost.creeperlauncher.minecraft.jsons.AssetIndexManifest;
import net.creeperhost.creeperlauncher.minecraft.jsons.VersionManifest;
import net.creeperhost.creeperlauncher.pack.CancellationToken;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static net.creeperhost.creeperlauncher.Constants.MC_RESOURCES;

/**
 * Update/downloads assets for a given Minecraft version.
 * <p>
 * Created by covers1624 on 17/11/21.
 */
public class InstallAssetsTask implements Task<Void> {

    // TODO, this should be moved to Settings, and swapped out when the thread limit changes.
    public static final ExecutorService POOL = new ThreadPoolExecutor(
            Settings.getThreadLimit(),
            Settings.getThreadLimit(),
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>()
    );

    private final List<NewDownloadTask> subTasks;

    public InstallAssetsTask(VersionManifest.AssetIndex assetIndex) throws IOException {
        subTasks = buildTaskList(assetIndex);
    }

    @Override
    public void execute(@Nullable CancellationToken token, @Nullable TaskProgressListener listener) throws Throwable {
        TaskProgressAggregator progressAggregator = null;
        if (listener != null) {
            long totalSize = subTasks.stream()
                    .map(NewDownloadTask::getValidation)
                    .mapToLong(DownloadValidation::expectedSize)
                    .sum();
            listener.start(totalSize);
            progressAggregator = new ParallelTaskProgressAggregator(listener);
        }

        ParallelTaskHelper.executeInParallel(token, POOL, subTasks, progressAggregator);

        if (listener != null) {
            listener.finish(progressAggregator.getProcessed());
        }
    }

    @Override
    public boolean isRedundant() {
        return subTasks.isEmpty();
    }

    @Nullable
    @Override
    public Void getResult() {
        return null;
    }

    /**
     * Build a linked list of tasks to Download/Update minecraft assets.
     *
     * @param assetIndex The asset index to download.
     * @return The list of tasks.
     */
    private static List<NewDownloadTask> buildTaskList(VersionManifest.AssetIndex assetIndex) throws IOException {
        Path assetsDir = Constants.BIN_LOCATION.resolve("assets");
        AssetIndexManifest manifest = AssetIndexManifest.update(assetsDir, assetIndex);

        Set<Path> seen = new HashSet<>();
        List<NewDownloadTask> tasks = new LinkedList<>();
        for (Map.Entry<String, AssetIndexManifest.AssetObject> entry : manifest.objects.entrySet()) {
            String name = entry.getKey();
            AssetIndexManifest.AssetObject object = entry.getValue();
            assert object.hash != null;

            String loc = object.hash.toString().substring(0, 2) + "/" + object.hash;
            Path dest;
            if (manifest.virtual) {
                dest = assetsDir.resolve("virtual").resolve(assetIndex.id).resolve(name);
            } else {
                dest = assetsDir.resolve("objects").resolve(loc);
            }

            // Some vanilla assets (.ico files) have the same hash
            // this causes duplicate tasks to be added.
            if (!seen.add(dest)) continue;

            NewDownloadTask task = new NewDownloadTask(
                    MC_RESOURCES + loc,
                    dest,
                    DownloadValidation.of()
                            .withExpectedSize(object.size)
                            .withHash(Hashing.sha1(), object.hash)
            );
            if (!task.isRedundant()) {
                tasks.add(task);
            }
        }

        return tasks;
    }

}
