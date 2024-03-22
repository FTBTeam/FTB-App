package net.creeperhost.creeperlauncher.storage.settings;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.util.ProxyUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Settings {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Type settingsTokenLegacy = new TypeToken<HashMap<String, String>>(){}.getType();    
    private static SettingsData settingsData = null;

    public static void saveSettings() {
        // Very defensive fallback system but allows us to save settings even if they weren't loaded.
        if (settingsData == null) {
            LOGGER.warn("No settings available at save time. Failing back to defaults.");
            if (settingsData == null) {
                settingsData = DEFAULT_SETTINGS;
            }
        }
        
        try {
            settingsData.write();
        } catch (Exception e) {
            LOGGER.error("Failed to save settings file.", e);
        }
    }
    
    public static void loadSettings() {
        // TODO: Remove in newer versions
        attemptMigration();
        if (Settings.settingsData != null) {
            // Migration happened, we don't need to load.

            // Again, this shouldn't be needed but it looks like it is...
            if (settingsData.instanceLocation().equals(Constants.getDataDir())) {
                Settings.settingsData.setInstanceLocation(Constants.INSTANCES_FOLDER_LOC);
            }
            
            return;
        }
            
        // Try and load the settings file
        try {
            Settings.settingsData = SettingsData.read();
            
            // Attempt to fix the instance location if it's fucked for some reason
            // I think something with path parsing might break it.
            if (settingsData.instanceLocation().equals(Constants.getDataDir())) {
                Settings.settingsData.setInstanceLocation(Constants.INSTANCES_FOLDER_LOC);   
            }
        } catch (Throwable e) {
            LOGGER.warn("Failed to load settings file, using defaults.", e);
            Settings.settingsData = Settings.DEFAULT_SETTINGS;
            
            // Backup the old settings file
            if (Files.exists(Constants.SETTINGS_FILE)) {
                try {
                    Files.move(Constants.SETTINGS_FILE, Constants.SETTINGS_FILE.resolveSibling("settings.json.bak"));
                } catch (IOException ex) {
                    LOGGER.warn("Failed to backup settings file.", ex);
                }
            }

            Settings.settingsData.write();
        }
        
        ProxyUtils.loadProxy();
    }

    public static void attemptMigration() {
        // Don't migrate if the new settings file exists or the old one doesn't
        if (Files.notExists(Constants.SETTINGS_FILE_LEGACY) || Files.exists(Constants.SETTINGS_FILE)) {
            return;
        }
        
        LOGGER.info("Migrating old settings to new format.");

        try {
            var originalSettingsRaw = Files.readString(Constants.SETTINGS_FILE_LEGACY);
            var originalSettings = new Gson().<HashMap<String, String>>fromJson(originalSettingsRaw, settingsTokenLegacy);
            var migrator = new SettingsMigrator(originalSettings);
            var newSettings = migrator.migrate();
            
            // Save the new settings
            newSettings.write();
            Settings.settingsData = newSettings;
            
            // Delete the old settings file
            try {
                Files.deleteIfExists(Constants.SETTINGS_FILE_LEGACY);
            } catch (IOException e) {
                LOGGER.error("Failed to delete old settings file.", e);
            }
        } catch (Throwable e) {
            LOGGER.error("Failed to migrate settings file. Aborting migration process.", e);
            LOGGER.info("Deleting old settings file. If possible");
            try {
                Files.deleteIfExists(Constants.SETTINGS_FILE_LEGACY);
            } catch (IOException ex) {
                LOGGER.error("Failed to delete old settings file.", ex);
            }
        }
    }
    
    public static SettingsData getSettings() {
        if (settingsData == null) {
            LOGGER.error("Failed to load settings, using defaults.");
            settingsData = DEFAULT_SETTINGS;
        }
        
        return settingsData;
    }
    
    public static void updateSettings(SettingsData data) {
        settingsData = data;
        saveSettings();
    }

    public static Path getInstancesDir() {
        return getSettings().instanceLocation();
    }

    public static int getDefaultThreadLimit()
    {
        int defaultThreads = (Runtime.getRuntime().availableProcessors() / 2) - 1;
        if(defaultThreads < 2) defaultThreads = 2;
        return defaultThreads;
    }

    public static long getSpeedLimit() {
        if (settingsData == null || settingsData.download() == null) {
            return 0;
        }
        
        return settingsData.download().speedLimit();
    }

    public static int getThreadLimit() {
        return settingsData.download().threadLimit();
    }
    
    public static final String DEFAULT_SPEC = "1.0.0";
    
    private static final SettingsData DEFAULT_SETTINGS = SettingsData.createDefault();
    
    public static class SettingsMigrator {
        private final Map<String, String> oldSettings;
        
        public SettingsMigrator(Map<String, String> oldSettings) {
            this.oldSettings = oldSettings;
        }

        /**
         * Migrate old settings to the new format.
         */
        private SettingsData migrate() {
            if (this.oldSettings == null) {
                // Something failed, fallback. We likely read the file wrong or an empty file
                return DEFAULT_SETTINGS;
            }
            
            return new SettingsData(
                DEFAULT_SPEC,
                getOrDefault("instanceLocation", input -> {
                    // This shouldn't be needed but it looks like it is...
                    var path = Path.of(input);
                    if (path.equals(Constants.getDataDir())) {
                        // Something borked...
                        LOGGER.error("Instance location is the same as the data directory. Using default.");
                        return Constants.INSTANCES_FOLDER_LOC;
                    }
                    
                    return path;
                }, DEFAULT_SETTINGS.instanceLocation()),
                new SettingsData.GeneralSettings(
                    getOrDefault("updateChannel", DEFAULT_SETTINGS.general().releaseChannel()),
                    getOrDefault("cacheLife", Integer::parseInt, DEFAULT_SETTINGS.general().cacheLife()),
                    getOrDefault("exitOverwolf", Boolean::parseBoolean, DEFAULT_SETTINGS.general().exitOverwolf()),
                    getOrDefault("verbose", Boolean::parseBoolean, DEFAULT_SETTINGS.general().verbose())
                ),
                new SettingsData.InstanceSettings(
                    getOrDefault("width", Integer::parseInt, DEFAULT_SETTINGS.instanceDefaults().width()),
                    getOrDefault("height", Integer::parseInt, DEFAULT_SETTINGS.instanceDefaults().height()),
                    getOrDefault("memory", Integer::parseInt, DEFAULT_SETTINGS.instanceDefaults().memory()),
                    getOrDefault("fullscreen", Boolean::parseBoolean, DEFAULT_SETTINGS.instanceDefaults().fullscreen()),
                    getOrDefault("updateChannel", DEFAULT_SETTINGS.instanceDefaults().updateChannel()),
                    getOrDefault("jvmargs", DEFAULT_SETTINGS.instanceDefaults().javaArgs()),
                    getOrDefault("shellArgs", DEFAULT_SETTINGS.instanceDefaults().shellArgs())
                ),
                new SettingsData.AppearanceSettings(
                    getOrDefault("useSystemWindowStyle", Boolean::parseBoolean, DEFAULT_SETTINGS.appearance().useSystemWindowStyle()),
                    getOrDefault("showAdverts", Boolean::parseBoolean, DEFAULT_SETTINGS.appearance().showAds())
                ),
                new SettingsData.ProxySettings(
                    getOrDefault("proxyType", DEFAULT_SETTINGS.proxy().type()),
                    getOrDefault("proxyHost", DEFAULT_SETTINGS.proxy().host()),
                    getOrDefault("proxyPort", Integer::parseInt, DEFAULT_SETTINGS.proxy().port()),
                    getOrDefault("proxyUser", DEFAULT_SETTINGS.proxy().username()),
                    getOrDefault("proxyPassword", DEFAULT_SETTINGS.proxy().password())
                ),
                new SettingsData.DownloadSettings(
                    getOrDefault("threadLimit", Integer::parseInt, DEFAULT_SETTINGS.download().threadLimit()),
                    getOrDefault("speedLimit", Long::parseLong, DEFAULT_SETTINGS.download().speedLimit())
                )
            );
        }
        
        private <T> T getOrDefault(String key, Function<String, T> transform, T defaultValue) {
            String value = this.oldSettings.get(key);
            if (value == null) {
                return defaultValue;
            }
            
            try {
                return transform.apply(value);
            } catch (Exception e) {
                LOGGER.warn("Failed to parse setting " + key + " with value " + value, e);
                return defaultValue;
            }
        }

        /**
         * No type conversion, just return the value or the default.
         */
        private String getOrDefault(String key, String defaultValue) {
            String value = this.oldSettings.get(key);
            if (value == null) {
                return defaultValue;
            }

            return value;
        }
    }
}
