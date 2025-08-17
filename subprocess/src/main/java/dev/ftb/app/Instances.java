package dev.ftb.app;

import com.google.gson.reflect.TypeToken;
import dev.ftb.app.data.modpack.ModpackVersionManifest;
import dev.ftb.app.instance.InstanceCategory;
import dev.ftb.app.pack.Instance;
import dev.ftb.app.storage.settings.Settings;
import dev.ftb.app.util.ElapsedTimer;
import dev.ftb.app.util.FileUtils;
import dev.ftb.app.util.GsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Pattern;

public class Instances {

    private static final Logger LOGGER = LogManager.getLogger();

    private static Map<UUID, Instance> instances = new HashMap<>();
    private static LinkedList<InstanceCategory> categories = new LinkedList<>();
    
    private static final TypeToken<LinkedList<InstanceCategory>> CATEGORIES_TYPE = new TypeToken<>() {};

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

    public static void refreshInstances() {
        ElapsedTimer totalTimer = new ElapsedTimer();
        Path instancesDir = Settings.getInstancesDir();

        LOGGER.info("Reloading instances..");
        instances.clear();

        if (!Files.exists(instancesDir)) {
            LOGGER.info("Instances directory missing, skipping..");
        } else {
            ElapsedTimer timer = new ElapsedTimer();
            List<Instance> loadedInstances = FileUtils.listDir(instancesDir)
                    .parallelStream()
                    .filter(e -> Files.isDirectory(e) && !e.getFileName().toString().startsWith("."))
                    .map(Instances::loadInstance)
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
            LOGGER.info("Loaded {} out of {} instances in {}.", instances.size(), loadedInstances.size(), timer.elapsedStr());
        }

        loadCategories();
        LOGGER.info("Finished instance reload in {}", totalTimer.elapsedStr());
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
            LinkedList<InstanceCategory> loadedCategories = GsonUtils.loadJson(categoriesFile, CATEGORIES_TYPE.getType());
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
            GsonUtils.saveJson(categoriesFile, categories, CATEGORIES_TYPE.getType());
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
            
            GsonUtils.saveJson(categoriesFile, newCategories, CATEGORIES_TYPE.getType());
        } catch (IOException e) {
            LOGGER.error("Failed to save migrated categories.", e);
        }
        
        categories.addAll(newCategories);
    }
}
