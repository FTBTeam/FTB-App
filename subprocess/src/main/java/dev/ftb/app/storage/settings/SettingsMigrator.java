package dev.ftb.app.storage.settings;

import dev.ftb.app.AppMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;

public class SettingsMigrator {
    private final Map<String, String> oldSettings;

    private static final SettingsData DEFAULT_SETTINGS = SettingsData.createDefault();
    private static final Logger LOGGER = LoggerFactory.getLogger(SettingsMigrator.class);
    
    public SettingsMigrator(Map<String, String> oldSettings) {
        this.oldSettings = oldSettings;
    }

    /**
     * Migrate old settings to the new format.
     */
    public SettingsData migrate() {
        if (this.oldSettings == null) {
            // Something failed, fallback. We likely read the file wrong or an empty file
            return DEFAULT_SETTINGS;
        }

        return new SettingsData(
            SettingsData.DEFAULT_SPEC,
            getOrDefault("instanceLocation", input -> {
                // This shouldn't be needed but it looks like it is...
                var path = Path.of(input);
                if (path.equals(AppMain.paths().workingDir())) {
                    // Something borked...
                    LOGGER.error("Instance location is the same as the data directory. Using default.");
                    return AppMain.paths().instancesDir();
                }

                return path;
            }, DEFAULT_SETTINGS.instanceLocation()),
            getOrDefault("enableFeralGameMode", Boolean::parseBoolean, DEFAULT_SETTINGS.enableFeralGameMode()),
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
                getOrDefault("preventMetaModInjection", Boolean::parseBoolean, DEFAULT_SETTINGS.instanceDefaults().preventMetaModInjection()),
                false, // There is no existing setting for this
                getOrDefault("shellArgs", DEFAULT_SETTINGS.instanceDefaults().shellArgs()),
                "" // There is no existing setting for this
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
