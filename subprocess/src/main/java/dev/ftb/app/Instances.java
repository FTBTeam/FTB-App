package dev.ftb.app;

import com.google.common.hash.HashCode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.ftb.app.accounts.AccountManager;
import dev.ftb.app.api.RcpApi;
import dev.ftb.app.data.InstanceSupportMeta;
import dev.ftb.app.data.modpack.ModpackVersionManifest;
import dev.ftb.app.instance.InstanceCategory;
import dev.ftb.app.minecraft.jsons.VersionManifest;
import dev.ftb.app.pack.Instance;
import dev.ftb.app.pack.InstanceFactory;
import dev.ftb.app.services.AppHttp;
import dev.ftb.app.services.AppThreads;
import dev.ftb.app.services.cache.AppCache;
import dev.ftb.app.storage.settings.Settings;
import dev.ftb.app.util.ElapsedTimer;
import dev.ftb.app.util.FileUtils;
import dev.ftb.app.util.JsonStore;
import net.covers1624.jdkutils.JdkInstallationManager;
import net.covers1624.quack.gson.HashCodeAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class Instances {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping()
        .registerTypeAdapter(VersionManifest.OS.class, new VersionManifest.OsDeserializer())
        .registerTypeAdapter(HashCode.class, new HashCodeAdapter())
        .create();
    
    private static final TypeToken<LinkedList<InstanceCategory>> CATEGORIES_TYPE = new TypeToken<>() {};
    
    private Map<UUID, Instance> instances = new HashMap<>();
    
    private final JsonStore<LinkedList<InstanceCategory>> categories;
    private final InstanceSupportMeta supportMeta;
    
    private final Settings settings;
    private final AppPaths appPaths;
    private final RcpApi rcpApi;
    private final AccountManager accounts;
    private final AppThreads appThreads;
    private final AppCache appCache;
    private final AppHttp appHttp;
    private final JdkInstallationManager jdkManager;
    
    public Instances(
        AppPaths appPaths,
        Settings settings,
        RcpApi rcpApi,
        AccountManager accounts,
        AppThreads appThreads,
        AppCache appCache,
        AppHttp appHttp,
        JdkInstallationManager jdkManager
    ) {
        this.appPaths = appPaths;
        this.settings = settings;
        this.rcpApi = rcpApi;
        this.accounts = accounts;
        this.appCache = appCache;
        this.appThreads = appThreads;
        this.appHttp = appHttp;
        this.jdkManager = jdkManager;
        
        this.categories = new JsonStore<>(
            settings.getInstancesDir().resolve("categories.json"),
            GSON,
            CATEGORIES_TYPE.getType(),
            () -> new LinkedList<>(List.of(InstanceCategory.DEFAULT))
        );
        
        this.supportMeta = new InstanceSupportMeta(appPaths, this.appHttp);
    }

    @Nullable
    public Instance getInstance(UUID uuid) {
        return instances.get(uuid);
    }

    public void addInstance(Instance instance) {
        instances.put(instance.getUuid(), instance);
    }

    public Collection<Instance> allInstances() {
        return Collections.unmodifiableCollection(instances.values());
    }
    
    public LinkedList<InstanceCategory> categories() {
        return categories.get();
    }

    public void refreshInstances() {
        ElapsedTimer totalTimer = new ElapsedTimer();
        Path instancesDir = this.settings.getInstancesDir();

        LOGGER.info("Reloading instances..");
        instances.clear();

        if (!Files.exists(instancesDir)) {
            LOGGER.info("Instances directory missing, skipping..");
        } else {
            ElapsedTimer timer = new ElapsedTimer();
            List<Instance> loadedInstances = FileUtils.listDir(instancesDir)
                    .parallelStream()
                    .filter(e -> Files.isDirectory(e) && !e.getFileName().toString().startsWith("."))
                    .map(this::loadInstance)
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

    @Nullable
    private Instance loadInstance(Path path) {
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

    @Nullable
    private Instance tryLoadInstance(Path path, Path json) throws IOException {
        var factory = new InstanceFactory(
            this.settings,
            this.appPaths,
            this.appThreads,
            this.appCache,
            this.appHttp,
            this.accounts,
            this,
            this.rcpApi,
            this.jdkManager
        );
        
        Instance localInstance = factory.load(path, json);
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

    private void loadCategories() {
        var categoriesFile = this.settings.getInstancesDir().resolve("categories.json");
        if (Files.notExists(categoriesFile)) {
            // Try and migrate
            migrateCategories();
            return;
        }
        
        try {
            this.categories.load();
        } catch (IOException e) {
            LOGGER.error("Failed to load instance categories from file: {}", categoriesFile, e);
        }
    }
    
    @Nullable
    public InstanceCategory addCategory(String categoryName) {
        var newCategory = new InstanceCategory(categoryName);
        try {
            // The name check runs under the store's lock so two concurrent adds can't both pass it.
            boolean added = this.categories.updateIf(
                existing -> existing.stream().noneMatch(e -> e.name().equalsIgnoreCase(categoryName)),
                existing -> existing.add(newCategory)
            );
            if (!added) {
                LOGGER.warn("Tried to add existing category: {}", categoryName);
                return null;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to add new category: {}", categoryName, e);
            return null;
        }
        
        return newCategory;
    }
    
    @Nullable
    public InstanceCategory updateCategory(UUID category, String newName) {
        var foundCategory = categories.get().stream()
            .filter(e -> e.uuid().equals(category))
            .findFirst();
        
        if (foundCategory.isEmpty()) {
            LOGGER.warn("Tried to update non-existing category: {}", category);
            return null;
        }
        
        try {
            // Checked under the store's lock so a concurrent rename or remove can't slip in between the check and the update.
            boolean updated = this.categories.updateIf(
                existing -> existing.stream().anyMatch(e -> e.uuid().equals(category))
                    && existing.stream().noneMatch(e -> e.name().equalsIgnoreCase(newName)),
                existing -> existing.stream()
                    .filter(e -> e.uuid().equals(category))
                    .findFirst()
                    .ifPresent(e -> e.setName(newName))
            );
            if (!updated) {
                LOGGER.warn("Tried to update category {} to existing name: {}", category, newName);
                return null;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to update category: {} to new name: {}", category, newName, e);
            return null;
        }
        
        LOGGER.info("Updated category {} to new name: {}", category, newName);
        return foundCategory.get();
    }
    
    public void removeCategory(UUID category) {
        var foundCategory = categories.get().stream()
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
        
        try {
            this.categories.update(existing -> {
                existing.removeIf(e -> e.uuid().equals(category));
            });
        } catch (IOException e) {
            LOGGER.error("Failed to remove category: {}", category, e);
        }

        LOGGER.info("Removed category: {}", foundCategory.get().name());
    }
    
    private void migrateCategories() {
        LinkedList<InstanceCategory> newCategories = new LinkedList<>();
        LOGGER.info("Migrating old instance categories to new format..");

        newCategories.add(InstanceCategory.DEFAULT);
        
        List<Instance> instancesToSave = new ArrayList<>();
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

            instancesToSave.add(instance);
        }
        
        try {
            this.categories.update(existing -> {
                existing.clear();
                existing.addAll(newCategories);
            });
            
            for (Instance instance : instancesToSave) {
                instance.saveJson();
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save migrated instance categories.", e);
        }
    }

    public InstanceSupportMeta supportMeta() {
        return supportMeta;
    }
}
