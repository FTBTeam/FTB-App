package dev.ftb.app.util;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import dev.ftb.app.Constants;
import dev.ftb.app.data.mod.CurseMetadata;
import dev.ftb.app.data.mod.ModManifest;
import dev.ftb.app.data.modpack.ModpackVersionModsManifest;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.net.DownloadAction;
import net.covers1624.quack.net.okhttp.OkHttpDownloadAction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class CurseMetadataCache {

    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Type TYPE = new TypeToken<Map<String, FileMetadata>>() { }.getType();

    private final Path file;
    private final Map<String, FileMetadata> metadata;

    // Cache per-run of failed requests to make things a tiny bit snappier.
    private final Set<String> failedCache = ConcurrentHashMap.newKeySet();

    public CurseMetadataCache(Path file) {
        this.file = file;
        Map<String, FileMetadata> metadata = null;
        if (Files.exists(file)) {
            try {
                metadata = JsonUtils.parse(GSON, file, TYPE);
            } catch (IOException | JsonParseException ex) {
                LOGGER.error("Failed to load storage.", ex);
            }
        }

        if (metadata == null) {
            metadata = new HashMap<>();
        }

        this.metadata = metadata;
    }

    public @Nullable CurseMetadata getCurseMeta(@Nullable ModpackVersionModsManifest.Mod mod, String murmur) {
        if (mod != null) {
            long projectId = mod.getCurseProject();
            long fileId = mod.getCurseFile();
            String name = mod.getName();
            String synopsis = mod.getSynopsis();
            String icon = mod.getIcon();
            String slug = mod.getCurseSlug();
            return CurseMetadata.full(projectId, fileId, name, slug, synopsis, icon);
        }

        FileMetadata metadata = queryMetadata(murmur);
        if (metadata != null) {
            return metadata.toCurseInfo();
        }
        return null;
    }

    public @Nullable CurseMetadata getCurseMeta(long curseProject, long curseFile) {
        ModManifest mod = null;
        try {
            mod = Constants.MOD_VERSION_CACHE.queryMod(curseProject).get();
        } catch (InterruptedException | ExecutionException ex) {
            LOGGER.warn("Failed to query mod version.", ex);
        }
        ModManifest.Version version = null;
        if (mod != null) {
            version = mod.findVersion(curseFile);
        }

        if (version == null) return CurseMetadata.full(curseProject, curseFile, null, null, null, null);

        var curseSlug = mod.getLinks()
            .stream()
            .filter(e -> e.getType().equals("curseforge"))
            .map(e -> {
                var parts = e.getLink().split("/");
                return parts[parts.length - 1];
            })
            .findFirst()
            .orElse(null);
        
        return CurseMetadata.full(
                curseProject,
                curseFile,
                mod.getName(),
                curseSlug,
                mod.getSynopsis(),
                !mod.getArt().isEmpty() ? mod.getArt().get(0).getUrl() : null // TODO, this is so dumb.
        );
    }

    /**
     * Find or query metadata for the given file hash.
     *
     * @param murmurHash The sha1 to lookup.
     * @return The metadata.
     */
    public @Nullable FileMetadata queryMetadata(String murmurHash) {
        FileMetadata metadata = findMetadata(murmurHash);
        if (metadata != null) return metadata;

        if (failedCache.contains(murmurHash)) return null;

        synchronized (murmurHash.intern()) {
            metadata = findMetadata(murmurHash);
            if (metadata != null) return metadata;

            metadata = query(murmurHash);
            if (metadata == null) {
                failedCache.add(murmurHash);
            }
            synchronized (this.metadata) {
                this.metadata.put(murmurHash, metadata);
                save();
            }
            return metadata;
        }
    }

    /**
     * Try and find existing metadata for the given file hash.
     *
     * @param sha1 The sha1.
     * @return The metadata. {@code null} if no metadata exists already.
     */
    public @Nullable FileMetadata findMetadata(String sha1) {
        return metadata.get(sha1);
    }

    private void save() {
        try {
            synchronized (metadata) {
                JsonUtils.write(GSON, file, metadata, TYPE);
            }
        } catch (IOException ex) {
            LOGGER.error("Failed to save storage.", ex);
        }
    }

    private static @Nullable FileMetadata query(String murmur) {
        if (murmur.equals("-1")) {
            return null;
        }
        
        StringWriter sw = new StringWriter();
        try {
            LOGGER.info("Querying metadata for {}", murmur);
            DownloadAction action = new OkHttpDownloadAction()
                    .setClient(Constants.httpClient())
                    .setUrl(ModpacksChUtils.getModEndpoint() + "lookup/" + murmur)
                    .setDest(sw);
            
            ModpacksChUtils.injectBearerHeader(action);
            action.execute();

            FileLookupResponse resp = JsonUtils.parse(GSON, sw.toString(), FileLookupResponse.class);
            if (resp == null) return null;

            return resp.meta;
        } catch (IOException | JsonParseException ex) {
            LOGGER.warn("Failed to query metadata for {}", murmur, ex);
            return null;
        }
    }

    // Only the useful things from /public/mod/lookup/<hash>
    public record FileMetadata(
            long fileId,
            String name,
            String synopsis,
            String icon,
            String curseSlug,
            long curseProject,
            long curseFile,
//            long stored,
            String filename
    ) {

        public CurseMetadata toCurseInfo() {
            return CurseMetadata.full(curseProject, curseFile, name, curseSlug, synopsis, icon);
        }
    }

    public record FileLookupResponse(String status, FileMetadata meta) {
    }
}
