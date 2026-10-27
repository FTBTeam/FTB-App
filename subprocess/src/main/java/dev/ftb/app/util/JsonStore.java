package dev.ftb.app.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/// A JSON file on disk backing a single data object of type `T`.
///
/// This implementation should be thread-safe, crash-save and resilient to corruption. It comes with built-in support 
/// for migrating older formats to newer ones, and for falling back to a backup or defaults when the file is unreadable.
///
/// @param <T> the type of the data held in the file
public final class JsonStore<T> {
    private static final Logger LOGGER = LogManager.getLogger();
    
    private static final DateTimeFormatter CORRUPT_SUFFIX_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    
    private final Path path;
    private final Path tmpPath;
    private final Path backupPath;
    
    private final Gson gson;
    private final Type type;
    
    private final Supplier<T> defaults;
    private final UnaryOperator<JsonElement> migrator;
    private final List<Consumer<T>> onChange = new LinkedList<>();
    
    /// Provides a thread-safe lock around IO operations.
    private final ReentrantLock lock = new ReentrantLock();

    /// Null until first loaded, set or read. The defaults are created lazily so that constructing a store never
    /// runs the supplier, which may itself depend on whatever owns this store.
    private volatile @Nullable T data;

    /// @see #JsonStore(Path, Gson, Type, Supplier, UnaryOperator)
    public JsonStore(Path path, Gson gson, Type type, Supplier<T> defaults) {
        this(path, gson, type, defaults, UnaryOperator.identity());
    }

    /// @param path where the file lives on disk
    /// @param gson the Gson instance used to read and write the data
    /// @param type the type of `T`, either a `Class` or a `TypeToken` type for generic types
    /// @param defaults creates the data used when there is no usable file on disk
    /// @param migrator applied to the raw JSON before it is converted to `T`, used to upgrade older formats
    public JsonStore(Path path, Gson gson, Type type, Supplier<T> defaults, UnaryOperator<JsonElement> migrator) {
        this.path = path;
        this.tmpPath = SafeFiles.tempPathFor(path);
        this.backupPath = siblingWithSuffix(".bak");
        this.gson = gson;
        this.type = type;
        this.defaults = defaults;
        this.migrator = migrator;
    }

    public Path path() {
        return path;
    }

    /// The current data. Holds the defaults until {@link #load()} has been called.
    public T get() {
        T current = data;
        if (current != null) {
            return current;
        }

        lock.lock();
        try {
            if (data == null) {
                data = defaults.get();
            }
            return data;
        } finally {
            lock.unlock();
        }
    }

    /// Replaces the data and saves it.
    /// This replaces the entire data object, not a specific field or partial update. Use {@link #update(Consumer)} for that.
    public void set(T value) throws IOException {
        lock.lock();
        try {
            data = value;
            saveLocked();
        } finally {
            lock.unlock();
        }
    }

    /// Mutates the data and saves it
    public void update(Consumer<T> mutator) throws IOException {
        lock.lock();
        try {
            mutator.accept(get());
            saveLocked();
        } finally {
            lock.unlock();
        }
    }

    /// Adds a listener that is called whenever the data is changed and saved. The listener is called after the save is complete.
    public void onChange(Consumer<T> listener) {
        onChange.add(listener);
    }
    
    /// Loads the data from disk doing it's best to recover from corruption or missing files. If the file is unreadable, 
    /// it will be moved to a quarantined location and the defaults will be used.
    /// 
    /// TODO: We should likely hold a "Max" number of quarantined files and delete the oldest ones when we exceed that number. 
    ///       Otherwise, a user could fill up their disk with corrupt files.
    ///
    /// @return what happened during the load
    public LoadResult load() throws IOException {
        lock.lock();
        try {
            if (Files.deleteIfExists(tmpPath)) {
                LOGGER.warn("Deleted leftover temp file {} from an interrupted save", tmpPath);
            }

            boolean foundCorrupt = false;
            if (Files.exists(path)) {
                T loaded = read(path);
                if (loaded != null) {
                    // We return here to avoid doing anything with the backup logic
                    data = loaded;
                    return LoadResult.LOADED;
                }
                quarantine(path);
                foundCorrupt = true;
            }

            // Attempt to load from the backup if it exists
            T loadedFromBackup = loadFromBackup();
            if (loadedFromBackup != null) {
                return LoadResult.RECOVERED_FROM_BACKUP;
            }

            // Nothing worked, fall back to the defaults and save them to disk. This will overwrite any existing file or backup.
            data = defaults.get();
            return foundCorrupt ? LoadResult.CORRUPT_RESET_TO_DEFAULTS : LoadResult.MISSING_USING_DEFAULTS;
        } finally {
            lock.unlock();
        }
    }
    
    @Nullable
    private T loadFromBackup() throws IOException {
        if (!Files.exists(backupPath)) {
            return null;
        }
        
        T loaded = read(backupPath);
        if (loaded != null) {
            LOGGER.warn("Recovered {} from backup {}", path, backupPath);
            data = loaded;
            try {
                saveLocked();
            } catch (IOException ex) {
                LOGGER.error("Failed to rewrite {} after recovering it from backup", path, ex);
            }
            return loaded;
        }
        quarantine(backupPath);
        return null;
    }

    public void save() throws IOException {
        lock.lock();
        try {
            saveLocked();
        } finally {
            lock.unlock();
        }
    }

    private void saveLocked() throws IOException {
        // Serialize first so a serialization failure never touches the files on disk
        byte[] bytes = gson.toJson(get(), type).getBytes(StandardCharsets.UTF_8);

        if (Files.exists(path)) {
            // Backup the existing one. Copied rather than moved, so the real file exists at every point during the save.
            try {
                Files.copy(path, backupPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ex) {
                LOGGER.warn("Failed to back up {} before saving", path, ex);
            }
        }

        SafeFiles.writeAtomically(path, bytes);
        
        // Assuming the save was successful, we can now notify any listeners
        for (Consumer<T> listener : onChange) {
            try {
                listener.accept(get());
            } catch (Throwable ex) {
                LOGGER.error("Failed to notify listener of change to {}", path, ex);
            }
        }
    }

    private @Nullable T read(Path file) throws IOException {
        String raw = Files.readString(file, StandardCharsets.UTF_8);
        if (raw.isBlank()) {
            LOGGER.warn("{} is empty", file);
            return null;
        }

        try {
            JsonElement element = JsonParser.parseString(raw);
            if (GsonUtils.isNull(element)) {
                LOGGER.warn("{} contains null", file);
                return null;
            }
            
            // Apply the migrator to the raw JSON before converting it to the data type. This allows us to upgrade older formats to newer ones.
            return gson.fromJson(migrator.apply(element), type);
        } catch (JsonParseException | IllegalStateException ex) {
            LOGGER.warn("Failed to parse {}", file, ex);
            return null;
        }
    }

    /// Moves the file to a quarantined location with a timestamped suffix. This is used when the file is unreadable or corrupt.
    private void quarantine(Path file) {
        Path target = file.resolveSibling(file.getFileName() + ".corrupt-" + LocalDateTime.now().format(CORRUPT_SUFFIX_FORMAT));
        try {
            Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.warn("Moved unreadable {} to {}", file, target);
        } catch (IOException ex) {
            LOGGER.error("Failed to quarantine unreadable {}", file, ex);
        }
    }

    private Path siblingWithSuffix(String suffix) {
        return path.resolveSibling(path.getFileName() + suffix);
    }

    /// The result of a load operation, likely almost always `LOADED` or just ignored by the implementor but it's good
    /// for debugging a failing load.
    /// 
    /// `LOADED` - The file was loaded successfully.
    /// `RECOVERED_FROM_BACKUP` - The file was unreadable but the backup was loaded successfully.
    /// `MISSING_USING_DEFAULTS` - The file was missing and the defaults were used.
    /// `CORRUPT_RESET_TO_DEFAULTS` - The file was unreadable and the defaults were used. The unreadable file was moved to a quarantined location
    public enum LoadResult {
        LOADED,
        RECOVERED_FROM_BACKUP,
        MISSING_USING_DEFAULTS,
        CORRUPT_RESET_TO_DEFAULTS
    }
}
