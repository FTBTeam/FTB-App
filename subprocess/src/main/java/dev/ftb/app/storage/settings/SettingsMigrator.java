package dev.ftb.app.storage.settings;

import dev.ftb.app.AppPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;

public class SettingsMigrator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SettingsMigrator.class);
    
    private final Map<String, String> oldSettings;
    private final AppPaths appPaths;
    
    public SettingsMigrator(AppPaths appPaths, Map<String, String> oldSettings) {
        this.appPaths = appPaths;
        this.oldSettings = oldSettings;
    }

    /**
     * Migrate old settings to the new format.
     */
    public SettingsData migrate() {
        var defaults = SettingsData.createDefault(this.appPaths);
        
        if (this.oldSettings == null) {
            // Something failed, fallback. We likely read the file wrong or an empty file
            return defaults;
        }

        
        return new SettingsData(
            SettingsData.DEFAULT_SPEC,
            getOrDefault("instanceLocation", input -> {
                // This shouldn't be needed but it looks like it is...
                var path = Path.of(input);
                if (path.equals(appPaths.workingDir())) {
                    // Something borked...
                    LOGGER.error("Instance location is the same as the data directory. Using default.");
                    return appPaths.instancesDir();
                }

                return path;
            }, defaults.instanceLocation()),
            getOrDefault("enableFeralGameMode", Boolean::parseBoolean, defaults.enableFeralGameMode()),
            new SettingsData.GeneralSettings(
                getOrDefault("updateChannel", defaults.general().releaseChannel()),
                getOrDefault("cacheLife", Integer::parseInt, defaults.general().cacheLife()),
                getOrDefault("exitOverwolf", Boolean::parseBoolean, defaults.general().exitOverwolf()),
                getOrDefault("verbose", Boolean::parseBoolean, defaults.general().verbose())
            ),
            new SettingsData.InstanceSettings(
                getOrDefault("width", Integer::parseInt, defaults.instanceDefaults().width()),
                getOrDefault("height", Integer::parseInt, defaults.instanceDefaults().height()),
                getOrDefault("memory", Integer::parseInt, defaults.instanceDefaults().memory()),
                getOrDefault("fullscreen", Boolean::parseBoolean, defaults.instanceDefaults().fullscreen()),
                getOrDefault("updateChannel", defaults.instanceDefaults().updateChannel()),
                getOrDefault("preventMetaModInjection", Boolean::parseBoolean, defaults.instanceDefaults().preventMetaModInjection()),
                false, // There is no existing setting for this
                getOrDefault("shellArgs", defaults.instanceDefaults().shellArgs()),
                "" // There is no existing setting for this
            ),
            new SettingsData.AppearanceSettings(
                getOrDefault("useSystemWindowStyle", Boolean::parseBoolean, defaults.appearance().useSystemWindowStyle()),
                getOrDefault("showAdverts", Boolean::parseBoolean, defaults.appearance().showAds())
            ),
            new SettingsData.ProxySettings(
                getOrDefault("proxyType", defaults.proxy().type()),
                getOrDefault("proxyHost", defaults.proxy().host()),
                getOrDefault("proxyPort", Integer::parseInt, defaults.proxy().port()),
                getOrDefault("proxyUser", defaults.proxy().username()),
                getOrDefault("proxyPassword", defaults.proxy().password())
            ),
            new SettingsData.DownloadSettings(
                getOrDefault("threadLimit", Integer::parseInt, defaults.download().threadLimit()),
                getOrDefault("speedLimit", Long::parseLong, defaults.download().speedLimit())
            ),
            new SettingsData.WorkaroundSettings(
                false
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
        } catch (Throwable e) {
            LOGGER.error("Failed to parse setting " + key + " with value " + value, e);
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
