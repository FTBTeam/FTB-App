package net.creeperhost.creeperlauncher.install.tasks;

import com.google.common.hash.HashCode;
import com.google.gson.reflect.TypeToken;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.util.FileUtils;
import net.creeperhost.creeperlauncher.util.GsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

@SuppressWarnings ("UnstableApiUsage")
public class LocalCache implements AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Type setType = new TypeToken<Set<HashCode>>() {}.getType();

    private final Path cacheLocation;
    private final Path cacheIndexFile;
    private final Set<HashCode> files = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private boolean isDirty = false;
    private boolean autoSaveEnabled = true;

    public LocalCache(Path cacheLocation) {
        this.cacheLocation = cacheLocation;
        if(FileUtils.createDirectories(cacheLocation) == null || !Files.exists(cacheLocation))
        {
            LOGGER.error("Unable to create instance directory");
        }

        cacheIndexFile = cacheLocation.resolve("index.json");

        if (Files.exists(cacheIndexFile)) {
            try {
                Set<HashCode> index = GsonUtils.loadJson(cacheIndexFile, setType);
                for (HashCode hash : index) {
                    Path file = cacheLocation.resolve(makePath(hash));
                    if (Files.notExists(file)) {
                        isDirty = true;
                        LOGGER.warn("File in cache index does not exist, ignoring.. {}:{}", hash, file);
                        continue;
                    }
                    files.add(hash);
                }
            } catch (IOException e) {
                LOGGER.error("Failed to load cache index.", e);
            }
        }
        save();
    }

    /**
     * Disables auto saving of the cache index.
     * This should only be used inside a Try-With-Resources, as to ensure auto save is re-enabled.
     */
    public void disableAutoSave() {
        autoSaveEnabled = false;
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
     * @return If the file was added to the cache.
     * @throws IllegalArgumentException If an SHA1 hash was not provided.
     */
    public boolean put(Path f, HashCode hash) throws IllegalArgumentException {
        if (hash.bits() != 160) throw new IllegalArgumentException("SHA1 hash not provided.");
        if (Files.notExists(f)) return false;// File doesn't exist.
        if (files.contains(hash)) return false;// File already cached.
        String path = makePath(hash);
        Path file = cacheLocation.resolve(path);
        try {
            Files.createDirectories(file.getParent());
            Files.copy(f, file, StandardCopyOption.REPLACE_EXISTING);
            synchronized (files) {
                files.add(hash);
                isDirty = true;
                save();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to add '{}' to local cache.", f.toAbsolutePath(), e);
            return false;
        }
        return true;
    }

    /**
     * Moves the given file into the cache.
     * <p>
     * This will use a move operation, causing the file to be deleted form its original location.
     *
     * @param f    The file to add.
     * @param hash The Hash of the file. Must be an SHA1 hash.
     * @return If the file was added to the cache.
     * @throws IllegalArgumentException If an SHA1 hash was not provided.
     */
    public boolean ingest(Path f, HashCode hash) throws IllegalArgumentException {
        if (hash.bits() != 160) throw new IllegalArgumentException("SHA1 hash not provided.");
        if (Files.notExists(f)) return false;// File doesn't exist.
        if (files.contains(hash)) return false;// File already cached.
        String path = makePath(hash);
        Path file = cacheLocation.resolve(path);
        try {
            Files.createDirectories(file.getParent());
            Files.move(f, file, StandardCopyOption.REPLACE_EXISTING);
            synchronized (files) {
                files.add(hash);
                isDirty = true;
                save();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to add '{}' to local cache.", f.toAbsolutePath(), e);
            return false;
        }
        return true;
    }

    /**
     * Removes any files that are too old from the cache.
     */
    public void clean() {
        long cacheLife = Long.parseLong(Settings.settings.getOrDefault("cacheLife", "5184000"));
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
    public void save() {
        if (!isDirty || !autoSaveEnabled) {
            return;
        }
        try {
            GsonUtils.saveJson(cacheIndexFile, files, setType);
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

    @Override
    public void close() {
        autoSaveEnabled = true;
        save();
    }
}
