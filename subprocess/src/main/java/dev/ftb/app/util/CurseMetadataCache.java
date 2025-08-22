package dev.ftb.app.util;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import dev.ftb.app.AppMain;
import dev.ftb.app.Constants;
import dev.ftb.app.data.mod.CurseMetadata;
import dev.ftb.app.data.mod.ModManifest;
import dev.ftb.app.data.modpack.ModpackVersionModsManifest;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.net.DownloadAction;
import net.covers1624.quack.net.okhttp.OkHttpDownloadAction;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class CurseMetadataCache {
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Type TYPE = new TypeToken<Map<String, FileMetadata>>() { }.getType();

    private static CurseMetadataCache instance;
    
    public static CurseMetadataCache get() {
        if (instance == null) {
            instance = new CurseMetadataCache(AppMain.paths().dataDir().resolve(".curse_meta.json"));
        }
        
        return instance;
    }
    
    private final Path file;
    private final Map<String, FileMetadata> metadata;

    // Cache per-run of failed requests to make things a tiny bit snappier.
    private final Set<String> failedCache = ConcurrentHashMap.newKeySet();

    private CurseMetadataCache(Path file) {
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
            return CurseMetadata.fromMod(mod);
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
            mod = ModVersionCache.get().queryMod(curseProject).get();
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
    
    public synchronized Map<String, FileMetadata> queryMetadata(String... murmurHashes) {
        var queryManifestData = new HashMap<String, FileMetadata>();
        for (var hash : murmurHashes) {
            var meta = findMetadata(hash);
            if (meta != null) {
                queryManifestData.put(hash, meta);
            }
        }
        
        var toQuery = new ArrayList<String>();
        for (var hash : murmurHashes) {
            if (failedCache.contains(hash)) continue;

            if (!queryManifestData.containsKey(hash) && !failedCache.contains(hash)) {
                toQuery.add(hash);
            }
        }
        
        if (toQuery.isEmpty()) {
            return queryManifestData;
        }

        var client = Constants.httpClient();
        var request = client.newCall(new Request.Builder()
            .url(Constants.FTB_MODPACKS_API + "/mod/lookup/hashes")
            .post(RequestBody.create(new Gson().toJson(Map.of("hashes", toQuery)), MediaType.parse("application/json")))
            .build());
        
        var mutated = false;
        try (var response = request.execute()) {
            var responseBody = response.body();
            if (!response.isSuccessful() || responseBody == null) {
                LOGGER.error("Failed to lookup mod hashes: {}", response.message());
                return null;
            }

            var responseJson = new Gson().fromJson(responseBody.string(), ModsLookupResponse.class);
            for (var lookup : responseJson.data()) {
                queryManifestData.put(String.valueOf(lookup.murmurHash()), lookup);
                synchronized (this.metadata) {
                    this.metadata.put(String.valueOf(lookup.murmurHash()), lookup);
                    mutated = true;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to create HTTP client for mod data lookup.", e);
        }
        
        if (mutated) {
            save();
        }
        
        return queryManifestData;
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
                    .setUrl(ModpackApiUtils.getModpacksApi() + "/mod/lookup/" + murmur)
                    .setDest(sw);
            
            ModpackApiUtils.injectBearerHeader(action);
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
            long stored,
            long murmurHash,
            String filename
    ) {
        public CurseMetadata toCurseInfo() {
            return CurseMetadata.full(curseProject, curseFile, name, curseSlug, synopsis, icon);
        }
    }

    public record ModsLookupResponse(
        String notice,
        String status,
        List<CurseMetadataCache.FileMetadata> data
    ) {}
    
    public record FileLookupResponse(String status, FileMetadata meta) {
    }

    /**
     * {
     *   "_notice": "This endpoint is deprecated and will be removed in the future. Please do not rely on this endpoint for any production code.",
     *   "status": "success",
     *   "meta": {
     *     "fileId": 6420931,
     *     "name": "EMI",
     *     "synopsis": "Featureful and accessible modern item and recipe viewer with JEI compatibility",
     *     "icon": "https://media.forgecdn.net/avatars/545/351/637878590194850929.png",
     *     "curseSlug": "emi",
     *     "curseProject": 580555,
     *     "curseFile": 6420931,
     *     "stored": 1744717501,
     *     "filename": "emi-1.1.22+1.21.1+neoforge.jar",
     *     "murmurHash": 1652373382
     *   }
     * }
     */

/**
 * {
 *   "_notice": "This endpoint is deprecated and will be removed in the future. Please do not rely on this endpoint for any production code.",
 *   "status": "success",
 *   "data": [
 *     {
 *       "fileId": 6420931,
 *       "name": "EMI",
 *       "synopsis": "Featureful and accessible modern item and recipe viewer with JEI compatibility",
 *       "icon": "https://media.forgecdn.net/avatars/545/351/637878590194850929.png",
 *       "curseSlug": "emi",
 *       "curseProject": 580555,
 *       "curseFile": 6420931,
 *       "stored": 1744717501,
 *       "filename": "emi-1.1.22+1.21.1+neoforge.jar",
 *       "murmurHash": 1652373382
 *     }
 *   ]
 * }
 */
}
