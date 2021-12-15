package net.creeperhost.creeperlauncher.install.tasks;

import com.google.common.hash.Hashing;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.install.tasks.NewDownloadTask.DownloadValidation;
import net.creeperhost.creeperlauncher.minecraft.jsons.AssetIndexManifest;
import net.creeperhost.creeperlauncher.minecraft.jsons.VersionManifest;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static net.creeperhost.creeperlauncher.Constants.MC_RESOURCES;

/**
 * Update/downloads assets for a given Minecraft version.
 * <p>
 * Created by covers1624 on 17/11/21.
 */
public class InstallAssetsTask implements Task<Void> {

    private final List<NewDownloadTask> subTasks;

    public InstallAssetsTask(VersionManifest.AssetIndex assetIndex) throws IOException {
        subTasks = buildTaskList(assetIndex);
    }

    @Override
    public void execute(@Nullable TaskProgressListener listener) throws Throwable {
        ProgressAggregator progressAggregator = null;
        if (listener != null) {
            long totalSize = subTasks.stream()
                    .map(NewDownloadTask::getValidation)
                    .mapToLong(DownloadValidation::expectedSize)
                    .sum();
            listener.start(totalSize);
            progressAggregator = new ProgressAggregator(listener);
        }

        for (NewDownloadTask subTask : subTasks) {
            subTask.execute(progressAggregator);
        }

        if (listener != null) {
            listener.finish(progressAggregator.processed);
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

    private static class ProgressAggregator implements TaskProgressListener {

        private final TaskProgressListener parent;

        private long processed = 0;

        private ProgressAggregator(TaskProgressListener parent) {
            this.parent = parent;
        }

        @Override
        public void update(long processed) {
            this.processed += processed;
            parent.update(this.processed);
        }

        //@formatter:off
        @Override public void start(long total) { } // We don't care about.
        @Override public void finish(long total) { } // Or this
        //@formatter:on
    }
}
