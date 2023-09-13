package net.creeperhost.creeperlauncher.data.modpack;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.io.IOUtils;
import net.covers1624.quack.net.DownloadAction;
import net.covers1624.quack.net.okhttp.OkHttpDownloadAction;
import net.creeperhost.creeperlauncher.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Created by covers1624 on 6/9/23.
 */
public class ModpackVersionModsManifest {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final Gson GSON = new Gson();

    private final List<Mod> mods = new LinkedList<>();

    public static @Nullable ModpackVersionModsManifest load(Path path) throws IOException, JsonParseException {
        if (Files.notExists(path)) return null;

        return JsonUtils.parse(GSON, path, ModpackVersionModsManifest.class);
    }

    public static void save(Path path, ModpackVersionModsManifest manifest) throws IOException {
        JsonUtils.write(GSON, IOUtils.makeParents(path), manifest);
    }

    public static ModpackVersionModsManifest query(long packId, long versionId, boolean isPrivate, byte packType) throws IOException, JsonParseException {
        String url = Constants.getModpacksEndpoint(isPrivate, packType) + packId + "/" + versionId + "/mods";
        LOGGER.info("Querying Modpack version mods manifest: {}", url);
        StringWriter sw = new StringWriter();
        DownloadAction action = new OkHttpDownloadAction()
                .setClient(Constants.httpClient())
                .setUrl(url)
                .setDest(sw);
        action.execute();

        return JsonUtils.parse(GSON, sw.toString(), ModpackVersionModsManifest.class);
    }

    public List<Mod> getMods() {
        return mods;
    }

    public @Nullable Mod getMod(long fileId) {
        return FastStream.of(mods)
                .filter(e -> e.fileId == fileId)
                .firstOrDefault();
    }

    public static class Mod {

        private long fileId;
        private @Nullable String name;
        private @Nullable String synopsis;
        private @Nullable String icon;
        private @Nullable String curseSlug;
        private long curseProject;
        private long curseFile;
        private long stored;
        private @Nullable String filename;

        // @formatter:off
        public long getFileId() { return fileId; }
        public String getName() { return Objects.requireNonNull(name); }
        public String getSynopsis() { return Objects.requireNonNull(synopsis); }
        public String getIcon() { return Objects.requireNonNull(icon); }
        public String getCurseSlug() { return Objects.requireNonNull(curseSlug); }
        public long getCurseProject() { return curseProject; }
        public long getCurseFile() { return curseFile; }
        public long getStored() { return stored; }
        public String getFilename() { return Objects.requireNonNull(filename); }
        // @formatter:on
    }
}
