package dev.ftb.app.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import dev.ftb.app.AppMain;
import dev.ftb.app.data.mod.ModManifest;
import net.covers1624.quack.annotation.NonNullApi;
import net.covers1624.quack.gson.JsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;

@NonNullApi
public class ModVersionCache {

    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Type TYPE = new TypeToken<Map<String, CacheEntry>>() { }.getType();

    private final Path file;
    private final Map<String, CacheEntry> modVersionCache;

    private final ExecutorService executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("ModVersionCache %d").setDaemon(true).build());
    private final Set<Long> modNegativeCache = ConcurrentHashMap.newKeySet();
    private final Set<String> versionNegativeCache = ConcurrentHashMap.newKeySet();
    private final LoadingCache<Long, ModManifest> modCache = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                @Override
                public ModManifest load(Long key) throws Exception {
                    try {
                        return ModManifest.queryManifest(key);
                    } catch (Throwable ex) {
                        throw new IllegalStateException("Failed to query mod manifest for: " + key, ex);
                    }
                }
            });

    @Nullable
    private static ModVersionCache instance;
    
    public static ModVersionCache get() {
        if (instance == null) {
            instance = new ModVersionCache(AppMain.paths().dataDir().resolve(".mod_meta.json"));
        }
        
        return instance;
    }
    
    private ModVersionCache(Path file) {
        this.file = file;
        Map<String, CacheEntry> modVersionCache = null;
        if (Files.exists(file)) {
            try {
                modVersionCache = JsonUtils.parse(GSON, file, TYPE);
            } catch (IOException | JsonParseException ex) {
                LOGGER.error("Failed to load ModVersion cache.", ex);
            }
        }

        boolean dirty = false;
        if (modVersionCache == null) {
            modVersionCache = new HashMap<>();
            dirty = true;
        } else {
            long currentTime = System.currentTimeMillis();
            for (Iterator<CacheEntry> iterator = modVersionCache.values().iterator(); iterator.hasNext(); ) {
                CacheEntry entry = iterator.next();
                if (currentTime > (entry.ts + TimeUnit.DAYS.toMillis(7))) {
                    iterator.remove();
                    dirty = true;
                }
            }
        }

        this.modVersionCache = modVersionCache;
        if (dirty) {
            save();
        }
    }

    /**
     * Query the given curse Mod ID, returning the value from local cache if available,
     * otherwise querying a fresh copy.
     * <p>
     * Note, the future may return a null value in the event that the API was unable
     * to find a mod with the given id.
     * <p>
     * Note2, These manifests do not get serialized to disk, they are cached briefly
     * in-ram only to avoid polling the API continually.
     *
     * @param modId The curse mod id.
     * @return The future.
     */
    public CompletableFuture<@Nullable ModManifest> queryMod(long modId) {
        // Nothing to do.
        if (modId < 1) return completedFuture(null);
        // Slight optimization, to prevent hammering the API with unknown mods. Not sure if this can even happen, but sure.
        if (modNegativeCache.contains(modId)) return completedFuture(null);

        // Speeed, don't context switch if we don't need to.
        ModManifest manifest = modCache.getIfPresent(modId);
        if (manifest != null) return completedFuture(manifest);

        return supplyAsync(() -> {
            try {
                return modCache.get(modId);
            } catch (ExecutionException | UncheckedExecutionException | ExecutionError ex) {
                LOGGER.error("Error whilst querying mod from ram cache.", ex);
                modNegativeCache.add(modId);
                return null;
            }
        }, executor);
    }

    /**
     * Query the specified mod version for the given mod. These will be cached for a period of time on disk
     * before needing to be re-queried.
     * <p>
     * Note, The future may return a null value in the event that the API was unable
     * to find a mod with the given id, or that mod did not have the version specified.
     *
     * @param modId      The curse mod ID.
     * @param modVersion The curse mod version ID.
     * @return The future.
     */
    public CompletableFuture<CachedMod> queryVersion(long modId, long modVersion) {
        String key = modId + ":" + modVersion;
        if (modId < 1 || modVersion < 1 || versionNegativeCache.contains(key)) return completedFuture(null);

        CacheEntry e = modVersionCache.get(key);
        if (e != null) return completedFuture(e.mod);

        return queryMod(modId).thenApplyAsync(mod -> {
            synchronized (modVersionCache) {
                // Mod does not exist.
                if (mod == null) {
                    versionNegativeCache.add(key);
                    return null;
                }

                // Yay, double-checking because locks!
                CacheEntry cacheEntry = modVersionCache.get(key);
                if (cacheEntry != null) return cacheEntry.mod;

                ModManifest.Version version = mod.findVersion(modVersion);
                // Oh well, nothing we can do..
                if (version == null) {
                    versionNegativeCache.add(key);
                    return null;
                }
                CachedMod cachedMod = new CachedMod(
                        mod.getName(),
                        mod.getDescription(),
                        mod.getSynopsis(),
                        version
                );
                modVersionCache.put(key, new CacheEntry(System.currentTimeMillis(), cachedMod));
                save();
                return cachedMod;
            }
        }, executor);
    }

    private synchronized void save() {
        try {
            JsonUtils.write(GSON, file, modVersionCache, TYPE);
        } catch (IOException ex) {
            LOGGER.warn("Failed to save mod cache.", ex);
        }
    }

    private record CacheEntry(long ts, CachedMod mod) {
    }

    public record CachedMod(String name, String synopsis, String description, ModManifest.Version version) {
    }
}
