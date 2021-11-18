package net.creeperhost.creeperlauncher.minecraft.jsons;

import com.google.common.hash.HashCode;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import net.covers1624.quack.gson.HashCodeAdapter;
import net.creeperhost.creeperlauncher.install.tasks.NewDownloadTask;
import net.creeperhost.creeperlauncher.util.GsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Asset index json.
 * <p>
 * Created by covers1624 on 17/11/21.
 */
public class AssetIndexManifest {

    private static final Logger LOGGER = LogManager.getLogger();
    public Map<String, AssetObject> objects = new HashMap<>();
    public boolean virtual;

    /**
     * Updates (if required) the asset index for the specified version.
     *
     * @param assetsDir  The path to the assets' directory on disk.
     * @param assetIndex The {@link VersionManifest.AssetIndex} to update for.
     * @return The {@link AssetIndexManifest} parsed from disk.
     * @throws IOException        Thrown when an error occurs whilst loading the manifest.
     * @throws JsonParseException Thrown when the Json cannot be parsed.
     */
    public static AssetIndexManifest update(Path assetsDir, VersionManifest.AssetIndex assetIndex) throws IOException {
        Path assetIndexFile = assetsDir.resolve("indexes/" + assetIndex.id + ".json");
        LOGGER.info("Updating AssetIndex Manifest for '{}' from '{}'.", assetIndex.id, assetIndex.url);

        NewDownloadTask downloadTask = new NewDownloadTask(
                assetIndex.url,
                assetIndexFile,
                NewDownloadTask.TaskValidation.sha1(assetIndex.size, assetIndex.sha1),
                null
        );

        if (!downloadTask.isRedundant()) {
            try {
                downloadTask.execute();
            } catch (Throwable e) {
                if (Files.exists(assetIndexFile)) {
                    LOGGER.warn("Failed to update AssetIndexManifest. Continuing with disk cache..", e);
                } else {
                    throw new IOException("Failed to update AssetIndexManifest. Disk cache does not exist.", e);
                }
            }
        }
        return GsonUtils.loadJson(assetIndexFile, AssetIndexManifest.class);
    }

    public static class AssetObject {

        @Nullable
        @JsonAdapter (HashCodeAdapter.class)
        public HashCode hash;
        public int size;
    }
}
