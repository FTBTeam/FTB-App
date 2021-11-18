package net.creeperhost.creeperlauncher.install.tasks;

import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.minecraft.jsons.AssetIndexManifest;
import net.creeperhost.creeperlauncher.minecraft.jsons.VersionManifest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Update/downloads assets for a given Minecraft version.
 * <p>
 * Created by covers1624 on 17/11/21.
 */
// TODO Progress
public class InstallAssetsTask {

    private static final String RESOURCES_URL = "https://resources.download.minecraft.net/";

    /**
     * Build a linked list of tasks to Download/Update minecraft assets.
     *
     * @param assetIndex The asset index to download.
     * @return The list of tasks.
     */
    public static List<Task<?>> build(VersionManifest.AssetIndex assetIndex) throws IOException {
        Path assetsDir = Constants.BIN_LOCATION.resolve("assets");
        AssetIndexManifest manifest = AssetIndexManifest.update(assetsDir, assetIndex);

        List<Task<?>> tasks = new LinkedList<>();
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
                    RESOURCES_URL + loc,
                    dest,
                    NewDownloadTask.TaskValidation.sha1(object.size, object.hash),
                    null
            );
            if (!task.isRedundant()) {
                tasks.add(task);
            }
        }

        return tasks;
    }
}
