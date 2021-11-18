package net.creeperhost.creeperlauncher.minecraft.jsons;

import com.google.gson.JsonParseException;
import net.creeperhost.creeperlauncher.install.tasks.NewDownloadTask;
import net.creeperhost.creeperlauncher.util.GsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static net.covers1624.quack.collection.ColUtils.onlyOrDefault;

/**
 * Minecraft 'Versions List' manifest.
 * <p>
 * Created by covers1624 on 8/11/21.
 */
public class VersionListManifest {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final String URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json";

    @Nullable
    public Latest latest;
    public List<Version> versions = new ArrayList<>();

    /**
     * Updates (if required) the version list manifest.
     *
     * @param versionsDir The versions directory.
     * @return The {@link VersionListManifest} parsed from disk.
     * @throws IOException        Thrown when an error occurs whilst loading the manifest.
     * @throws JsonParseException Thrown when the Json cannot be parsed.
     */
    public static VersionListManifest update(Path versionsDir) throws IOException {
        Path versionsFile = versionsDir.resolve("version_manifest.json");
        NewDownloadTask downloadTask = new NewDownloadTask(
                URL,
                versionsFile,
                NewDownloadTask.TaskValidation.none(),
                null
        );

        if (!downloadTask.isRedundant()) {
            try {
                downloadTask.execute();
            } catch (Throwable e) {
                if (Files.exists(versionsFile)) {
                    LOGGER.warn("Failed to update VersionListManifest. Continuing with disk cache..", e);
                } else {
                    throw new IOException("Failed to update VersionListManifest. Disk cache does not exist.", e);
                }
            }
        }
        return GsonUtils.loadJson(versionsFile, VersionListManifest.class);
    }

    /**
     * Locate a specific {@link Version} from
     * a specific ID.
     *
     * @param id The ID to find the version for.
     * @return The found {@link Version} or <code>null</code>
     */
    @Nullable
    public Version locate(String id) {
        return onlyOrDefault(versions.stream().filter(e -> e.id.equalsIgnoreCase(id)), null);
    }

    public static class Latest {

        public String release;
        public String snapshot;
    }

    public static class Version {

        public String id;
        public String type;
        public String url;
        public String time;
        public String releaseTime;
    }

}
