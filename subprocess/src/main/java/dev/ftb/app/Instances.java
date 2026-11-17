package dev.ftb.app;

import com.google.common.hash.HashCode;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.ftb.app.data.modpack.ModpackVersionManifest;
import dev.ftb.app.instance.InstanceCategory;
import dev.ftb.app.minecraft.jsons.VersionManifest;
import dev.ftb.app.pack.Instance;
import dev.ftb.app.storage.settings.Settings;
import dev.ftb.app.util.FileUtils;
import dev.ftb.app.util.GsonUtils;
import dev.ftb.app.util.MiscUtils;
import net.covers1624.quack.gson.HashCodeAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Instances {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping()
        .registerTypeAdapter(VersionManifest.OS.class, new VersionManifest.OsDeserializer())
        .registerTypeAdapter(HashCode.class, new HashCodeAdapter())
        .create();
    
    private static Map<UUID, Instance> instances = new HashMap<>();
    private static LinkedList<InstanceCategory> categories = new LinkedList<>();
    
    private static final TypeToken<LinkedList<InstanceCategory>> CATEGORIES_TYPE = new TypeToken<>() {};

    private static final Duration SLOW_INSTANCE_LOAD = Duration.ofSeconds(2);

    /// Single low priority thread for instance processes like computing hashes on the fly.
    private static final ExecutorService MAINTENANCE_EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
        .setNameFormat("Instance Maintenance")
        .setDaemon(true)
        .setPriority(Thread.MIN_PRIORITY)
        .build());

    @Nullable
    public static Instance getInstance(UUID uuid) {
        return Instances.instances.get(uuid);
    }

    public static void addInstance(Instance instance) {
        instances.put(instance.getUuid(), instance);
    }

    public static Collection<Instance> allInstances() {
        return Collections.unmodifiableCollection(Instances.instances.values());
    }
    
    public static LinkedList<InstanceCategory> categories() {
        return categories;
    }

    /// Queue instances for murmur hashing fixes. 
    public static void scheduleMurmurFixing() {
        List<UUID> pending = allInstances().stream()
            .filter(e -> e.getModifications() != null && e.getModifications().requiresMurmurFix())
            .map(Instance::getUuid)
            .toList();
        if (pending.isEmpty()) return;

        LOGGER.info("Queueing mod hash backfill for {} instance(s).", pending.size());
        MAINTENANCE_EXECUTOR.execute(() -> {
            for (UUID uuid : pending) {
                Instance instance = getInstance(uuid);
                if (instance == null) continue;
                try {
                    instance.ensureMurmurHashes();
                } catch (Throwable t) {
                    // Flag stays set, so this is retried on demand or on next startup.
                    LOGGER.warn("Failed to backfill mod hashes for instance {}.", instance.getName(), t);
                }
            }
        });
    }

    public static void refreshInstances() {
        long start = System.nanoTime();
        Path instancesDir = Settings.getInstancesDir();

        LOGGER.info("Reloading instances..");
        instances.clear();

        if (!Files.exists(instancesDir)) {
            LOGGER.info("Instances directory missing, skipping..");
        } else {
            List<Path> instancePaths = FileUtils.listDir(instancesDir).stream()
                    .filter(e -> Files.isDirectory(e) && !e.getFileName().toString().startsWith("."))
                    .toList();

            // Load in parallel, but time each instance so a slow one is named in the logs.
            List<Instance> loadedInstances = instancePaths.parallelStream()
                    .map(instancePath -> {
                        var timed = MiscUtils.time(() -> loadInstance(instancePath));
                        if (timed.elapsed().compareTo(SLOW_INSTANCE_LOAD) > 0) {
                            LOGGER.warn("Slow instance load: {} took {}", instancePath.getFileName(), timed.asString());
                        } else {
                            LOGGER.debug("Loaded instance {} in {}", instancePath.getFileName(), timed.asString());
                        }
                        return timed.result(); // null for skipped/broken instances, already logged by loadInstance
                    })
                    .filter(Objects::nonNull)
                    .toList();

            instances = new HashMap<>();
            for (Instance instance : loadedInstances) {
                // TODO, there is probably a better solution to this.
                if (instances.containsKey(instance.getUuid())) {
                    LOGGER.warn("Found duplicate instance {} with id {}. Ignoring.", instance.getDir(), instance.getUuid());
                    continue;
                }
                
                instances.put(instance.getUuid(), instance);
            }
            LOGGER.info("Loaded {} out of {} instances in {}.", instances.size(), loadedInstances.size(), MiscUtils.formatDuration(Duration.ofNanos(System.nanoTime() - start)));
        }

        LOGGER.info("Loading categories");
        loadCategories();
        LOGGER.info("Finished instance reload in {}", MiscUtils.formatDuration(Duration.ofNanos(System.nanoTime() - start)));
    }

    private static Instance loadInstance(Path path) {
        Path realJson = path.resolve("instance.json");
        Path backupJson = path.resolve("instance.json.bak");
        if (Files.notExists(realJson) && Files.notExists(backupJson)) {
            LOGGER.warn("Instance missing 'instance.json', Ignoring. {}", realJson.toAbsolutePath());
            return null;
        }
        try {
            return tryLoadInstance(path, realJson);
        } catch (Throwable ex) {
            if (Files.notExists(backupJson)) {
                LOGGER.error("Failed to load instance: {}", realJson.toAbsolutePath(), ex);
                return null;
            }
            LOGGER.warn("Failed to load instance via real json {}. Trying backup..", realJson.toAbsolutePath(), ex);
            try {
                Instance instance = tryLoadInstance(path, backupJson);
                LOGGER.info("Loading backup json successful!");
                try {
                    LOGGER.info("Restoring real json from backup.");
                    Files.copy(backupJson, realJson, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.info("Real json restored!");
                } catch (IOException ex2) {
                    LOGGER.error("Failed to restore backup json.", ex2);
                }

                return instance;
            } catch (Throwable ex2) {
                ex.addSuppressed(ex2); // Log and report first exception, with second attached as suppressed.
                LOGGER.error("Also failed to load instance via backup json.: {}", realJson.toAbsolutePath(), ex);
                return null;
            }
        }
    }

    private static Instance tryLoadInstance(Path path, Path json) throws IOException {
        Instance localInstance = new Instance(path, json);
        if (!localInstance.props.installComplete) {
            // TODO we should provide a cleanup function somewhere to remove these old installs, probably next to our cache flush button.
            LOGGER.warn("Instance install never completed, Ignoring. {}", json.toAbsolutePath());
            return null;
        }
        if (localInstance.getId() == ModpackVersionManifest.INVALID_ID) {
            // TODO, not really sure how an instance can get into this state at the moment.
            //       but instead of generating a sentry event for the error message, we emit a warning and ignore the instance.
            LOGGER.warn("Instance install complete and missing 'version.json', Ignoring. {}", json.toAbsolutePath());
            return null;
        }
        return localInstance;
    }

    private static void loadCategories() {
        categories.clear();
        
        var categoriesFile = Settings.getInstancesDir().resolve("categories.json");
        if (Files.notExists(categoriesFile)) {
            // Try and migrate
            migrateCategories();
            return;
        }
        
        // Load the categories from the file.
        try {
            LinkedList<InstanceCategory> loadedCategories = GSON.fromJson(Files.newBufferedReader(categoriesFile), CATEGORIES_TYPE.getType());
            if (loadedCategories == null) {
                LOGGER.warn("Failed to load instance categories from file: {}", categoriesFile);
                return;
            }
            
            categories = new LinkedList<>(loadedCategories);
        } catch (IOException e) {
            LOGGER.error("Failed to load instance categories from file: {}", categoriesFile, e);
        }
    }
    
    public static void saveCategories() {
        var categoriesFile = Settings.getInstancesDir().resolve("categories.json");
        try {
            if (Files.notExists(categoriesFile.getParent())) {
                Files.createDirectories(categoriesFile.getParent());
            }
            GsonUtils.saveJson(GSON, categoriesFile, categories, CATEGORIES_TYPE.getType());
        } catch (IOException e) {
            LOGGER.error("Failed to save instance categories to file: {}", categoriesFile, e);
        }
    }
    
    @Nullable
    public static InstanceCategory addCategory(String categoryName) {
        if (categories.stream().anyMatch(e -> e.name().equalsIgnoreCase(categoryName))) {
            LOGGER.warn("Tried to add existing category: {}", categoryName);
            return null;
        }
        
        var newCategory = new InstanceCategory(categoryName);
        categories.add(newCategory);
        saveCategories();
        
        return newCategory;
    }
    
    public static InstanceCategory updateCategory(UUID category, String newName) {
        var foundCategory = categories.stream()
            .filter(e -> e.uuid().equals(category))
            .findFirst();
        
        if (foundCategory.isEmpty()) {
            LOGGER.warn("Tried to update non-existing category: {}", category);
            return null;
        }
        
        // Check if the new name already exists.
        if (categories.stream().anyMatch(e -> e.name().equalsIgnoreCase(newName))) {
            LOGGER.warn("Tried to update category to existing name: {}", newName);
            return null;
        }
        
        // Update the category name.
        foundCategory.get().setName(newName);
        saveCategories();
        
        LOGGER.info("Updated category {} to new name: {}", category, newName);
        return foundCategory.get();
    }
    
    public static void removeCategory(UUID category) {
        var foundCategory = categories.stream()
            .filter(e -> e.uuid().equals(category))
            .findFirst();
        
        if (foundCategory.isEmpty()) {
            LOGGER.warn("Tried to remove non-existing category: {}", category);
            return;
        }
        
        // Move any instances in this category back to the default category.
        for (Instance instance : allInstances()) {
            if (instance.props.categoryId != null && instance.props.categoryId.equals(category)) {
                instance.props.categoryId = InstanceCategory.DEFAULT.uuid();
                try {
                    instance.saveJson();
                } catch (IOException e) {
                    LOGGER.error("Failed to save instance with new category id: {}", instance.getDir(), e);
                }
            }
        }
        
        // Remove the category from the list.
        categories.removeIf(e -> e.uuid().equals(category));
        
        LOGGER.info("Removed category: {}", foundCategory.get().name());
        saveCategories();
    }
    
    // TODO: Should this be a migration as part of the migration process?
    private static void migrateCategories() {
        LinkedList<InstanceCategory> newCategories = new LinkedList<>();
        LOGGER.info("Migrating old instance categories to new format..");

        newCategories.add(InstanceCategory.DEFAULT);
        
        for (Instance instance : allInstances()) {
            if (instance.props == null || instance.props.categoryId != null) {
                continue;
            }
            
            var categoryName = instance.props.category;
            Optional<InstanceCategory> existingCategory;
            if (!categoryName.isEmpty()) {
                existingCategory = newCategories.stream()
                    .filter(e -> e.name().equalsIgnoreCase(categoryName))
                    .findFirst();
            } else {
                existingCategory = Optional.empty();
            }

            instance.props.categoryId = existingCategory
                .map(InstanceCategory::uuid)
                .orElseGet(() -> {
                    InstanceCategory newCategory = new InstanceCategory(categoryName);
                    newCategories.add(newCategory);
                    return newCategory.uuid();
                });
            
            try {
                instance.saveJson();
            } catch (IOException e) {
                LOGGER.error("Failed to save instance with new category id: {}", instance.getDir(), e);
            }
        }
        
        // Save the categories to the file.
        try {
            Path categoriesFile = Settings.getInstancesDir().resolve("categories.json");
            if (Files.notExists(categoriesFile.getParent())) {
                Files.createDirectories(categoriesFile.getParent());
            }
            
            GsonUtils.saveJson(GSON, categoriesFile, newCategories, CATEGORIES_TYPE.getType());
        } catch (IOException e) {
            LOGGER.error("Failed to save migrated categories.", e);
        }
        
        categories.addAll(newCategories);
    }
}
