package dev.ftb.app.storage.settings;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.ftb.app.AppPaths;
import dev.ftb.app.util.JsonStore;
import dev.ftb.app.util.ProxyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

public class Settings {
    private static final Logger LOGGER = LoggerFactory.getLogger(Settings.class);
    private static final Type LEGACY_SETTINGS_TOKEN = new TypeToken<HashMap<String, String>>(){}.getType();
    
    private static final Gson GSON = new Gson().newBuilder()
        .setPrettyPrinting()
        .create();
    
    private final AppPaths appPaths;
    
    private final JsonStore<SettingsData> store;

    public Settings(AppPaths appPaths) {
        this.appPaths = appPaths;
        
        this.store = new JsonStore<>(
            appPaths.settingsFile(),
            GSON,
            SettingsData.class,
            () -> SettingsData.createDefault(this.appPaths)
        );

        attemptMigration();
        this.load();
    }
    
    public void save() {
        try {
            this.store.save();
        } catch (IOException e) {
            LOGGER.error("Failed to save settings file.", e);
        }
    }
    
    public void load() {
        try {
            this.store.load();
            ProxyUtils.loadProxy(); // Push update to proxy settings after loading
        } catch (IOException e) {
            LOGGER.error("Failed to load settings file. Falling back to default.", e);
        }
    }

    private void attemptMigration() {
        // Don't migrate if the new settings file exists or the old one doesn't
        Path legacySettingsFile = this.appPaths.legacySettingsFile();
        if (Files.notExists(legacySettingsFile) || Files.exists(this.appPaths.settingsFile())) {
            return;
        }
        
        LOGGER.info("Migrating old settings to new format.");
        try {
            var originalSettingsRaw = Files.readString(legacySettingsFile);
            var originalSettings = new Gson().<HashMap<String, String>>fromJson(originalSettingsRaw, LEGACY_SETTINGS_TOKEN);
            var migrator = new SettingsMigrator(this.appPaths, originalSettings);
            var newSettings = migrator.migrate();
            
            // Save the new settings
            this.store.set(newSettings);
            
            // Delete the old settings file
            try {
                Files.deleteIfExists(legacySettingsFile);
            } catch (IOException e) {
                LOGGER.error("Failed to delete old settings file.", e);
            }
        } catch (Throwable e) {
            LOGGER.error("Failed to migrate settings file. Aborting migration process.", e);
            LOGGER.info("Deleting old settings file. If possible");
            try {
                Files.deleteIfExists(legacySettingsFile);
            } catch (IOException ex) {
                LOGGER.error("Failed to delete old settings file.", ex);
            }
        }
    }
    
    public SettingsData settings() {
        return this.store.get();
    }

    public JsonStore<SettingsData> store() {
        return store;
    }
    
    public Path getInstancesDir() {
        return settings().instanceLocation();
    }

    public static int getDefaultThreadLimit() {
        int defaultThreads = (Runtime.getRuntime().availableProcessors() / 2) - 1;
        if(defaultThreads < 2) defaultThreads = 2;
        return defaultThreads;
    }

    public long getSpeedLimit() {        
        return settings().download().speedLimit();
    }

    public int getThreadLimit() {
        return settings().download().threadLimit();
    }

    public boolean ignoreForgeProcessorOutputHashes() {
        return settings().workaround().ignoreForgeProcessorOutputHashes();
    }
}
