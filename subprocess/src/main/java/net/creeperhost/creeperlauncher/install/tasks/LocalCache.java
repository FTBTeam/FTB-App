package net.creeperhost.creeperlauncher.install.tasks;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.covers1624.quack.gson.HashCodeAdapter;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.util.HashUtils;
import net.covers1624.quack.util.MultiHasher.HashFunc;
import net.creeperhost.creeperlauncher.install.FileValidation;
import net.creeperhost.creeperlauncher.storage.settings.Settings;
import net.creeperhost.creeperlauncher.util.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings ({ "deprecation", "UnstableApiUsage" })
public class LocalCache implements DownloadTask.LocalFileLocator {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(HashCode.class, new HashCodeAdapter())
            .create();
    private static final Type SET_TYPE = new TypeToken<Set<HashCode>>() { }.getType();

    private final Path cacheLocation;
    private final Path cacheIndexFile;
    private final Set<HashCode> files = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private boolean isDirty = false;

    public LocalCache(Path cacheLocation) {
        this.cacheLocation = cacheLocation;
        if (FileUtils.createDirectories(cacheLocation) == null || !Files.exists(cacheLocation)) {
            LOGGER.error("Unable to create instance directory");
        }

        cacheIndexFile = cacheLocation.resolve("index.json");

        if (Files.exists(cacheIndexFile)) {
            try {
                Set<HashCode> index = JsonUtils.parse(GSON, cacheIndexFile, SET_TYPE);
                for (HashCode hash : index) {
                    Path file = cacheLocation.resolve(makePath(hash));
                    if (Files.notExists(file)) {
                        isDirty = true;
                        LOGGER.warn("File in cache index does not exist, ignoring.. {}:{}", hash, file);
                        continue;
                    }
                    files.add(hash);
                }
            } catch (Throwable e) {
                LOGGER.error("Failed to load cache index.", e);
            }
        }
        save();
    }

    /**
     * Checks if the given hash exists in the cache.
     *
     * @param hash The hash to check.
     * @return If the file exists in the cache.
     */
    public boolean exists(HashCode hash) {
        return files.contains(hash);
    }

    /**
     * Gets the path for a given Hash.
     *
     * @param hash The hash.
     * @return The path or null.
     */
    @Nullable
    public Path get(HashCode hash) {
        if (!files.contains(hash)) return null;
        String path = makePath(hash);

        Path file = cacheLocation.resolve(path);
        if (Files.notExists(file)) {//Should never happen.
            LOGGER.warn("Removing stale cache entry for {}:{}", hash, file);
            synchronized (files) {
                files.remove(hash);
                isDirty = true;
                save();
            }
            return null;
        }
        return file;
    }

    /**
     * Adds a file to the cache.
     *
     * @param f    The file to add.
     * @param hash The Hash of the file. Must be an SHA1 hash.
     * @throws IllegalArgumentException If an SHA1 hash was not provided.
     */
    public void put(Path f, HashCode hash) throws IllegalArgumentException {
        if (hash.bits() != 160) throw new IllegalArgumentException("SHA1 hash not provided.");
        if (Files.notExists(f)) return;// File doesn't exist.
        if (files.contains(hash)) return;// File already cached.
        String path = makePath(hash);
        Path file = cacheLocation.resolve(path);

        // Alright.. the file already exists in cache, but not in our files set?
        // TODO, detect these in validation pass on load.
        if (Files.exists(file)) {
            try {
                LOGGER.warn("Existing file did not exist in LocalCache lookup? {}", hash);
                // The file has become corrupt..
                if (!HashUtils.hash(Hashing.sha1(), file).equals(hash)) {
                    // TODO, run a validation pass over LocalCache on load if this is a real problem.
                    LOGGER.error("Corrupt file in LocalStorage! {} does not match its expected hash of {}", file, hash);
                    return;
                }
                // Yaaay sha1 hash collisions!
                long aLen = Files.size(file);
                long bLen = Files.size(f);
                if (aLen != bLen) {
                    LOGGER.fatal("Hash collision adding to local cache. Hash {}, A: {}, {} B: {}, {}", hash, file, aLen, f, bLen);
                    return;
                }

                // Well, the file exists, size and hash match, just add.
                addAndSave(hash);
            } catch (IOException ex) {
                LOGGER.error("Failed to do LocalStorage.add pre-checks.", ex);
            }
            return;
        }

        try {
            // dfuq? But okay..
            Path parent = file.getParent();
            if (Files.exists(parent) && !Files.isDirectory(parent)) {
                try {
                    Files.delete(parent);
                } catch (IOException ex) {
                    LOGGER.warn("Parent already existed and was not a directory. Failed to delete..", ex);
                    return;
                }
            }
            Files.createDirectories(file.getParent());
            Files.copy(f, file, StandardCopyOption.REPLACE_EXISTING);
            addAndSave(hash);
        } catch (IOException e) {
            LOGGER.error("Failed to add '{}' to local cache.", f.toAbsolutePath(), e);
        }
    }

    private void addAndSave(HashCode hash) {
        synchronized (files) {
            files.add(hash);
            isDirty = true;
            save();
        }
    }

    /**
     * Removes any files that are too old from the cache.
     */
    public void clean() {
        long cacheLife = Settings.getSettings().general().cacheLife();
        if (cacheLife < 0) cacheLife = 900L;
        synchronized (files) {
            Instant now = Instant.now();
            for (Iterator<HashCode> iterator = files.iterator(); iterator.hasNext(); ) {
                HashCode hash = iterator.next();
                String path = makePath(hash);
                Path file = cacheLocation.resolve(path);
                if (shouldPurge(file, now, cacheLife)) {
                    try {
                        isDirty = true;
                        Files.deleteIfExists(file);
                        iterator.remove();
                    } catch (IOException ignored) {
                    }
                }
            }
            save();
        }
    }

    /**
     * Flushes the Cache index to disk if it has been marked as dirty.
     */
    private void save() {
        if (!isDirty) return;

        try {
            JsonUtils.write(GSON, cacheIndexFile, files, SET_TYPE);
        } catch (IOException e) {
            LOGGER.error("Failed to save cache index.", e);
        }
    }

    /**
     * Checks if the given file should be purged from the Cache.
     *
     * @param file      The file to check.
     * @param now       The time right now.
     * @param cacheLife The maximum age of a file.
     * @return If the file should be purged.
     */
    private boolean shouldPurge(Path file, Instant now, long cacheLife) {
        if (Files.notExists(file)) {
            return true;
        }
        BasicFileAttributes attrs;
        try {
            attrs = Files.readAttributes(file, BasicFileAttributes.class);
        } catch (IOException e) {
            LOGGER.warn("Unable to load attributes of file. {}", file.toAbsolutePath(), e);
            return true;
        }
        FileTime time = attrs.lastAccessTime();
        if (time == null) time = attrs.creationTime();
        Duration age = Duration.between(time.toInstant(), now);
        return !age.isNegative() && (age.toMillis() / 1000) > cacheLife;
    }

    /**
     * Generates the path for a given {@link HashCode}.
     * <p>
     * Path is generated using the first 2 digits of the Hash as a folder,
     * then followed by the entire hash as the file name.
     * <p>
     * This is similar to how Minecraft stores Assets, and is used to cut down
     * on metadata size of the root directory.
     *
     * @param hashCode The hash.
     * @return The path.
     */
    private static String makePath(HashCode hashCode) {
        String hash = hashCode.toString();
        return hash.substring(0, 2) + "/" + hash;
    }

    @Nullable
    @Override
    public Path getLocalFile(FileValidation validation, Path dest) {
        HashCode expectedSha1 = validation.expectedHashes.get(HashFunc.SHA1);
        return expectedSha1 == null ? null : get(expectedSha1);

    }

    @Override
    public void onFileDownloaded(FileValidation validation, Path dest) {
        HashCode expectedSha1 = validation.expectedHashes.get(HashFunc.SHA1);
        if (expectedSha1 != null) {
            put(dest, expectedSha1);
        }
    }
}
