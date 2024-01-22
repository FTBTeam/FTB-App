package net.creeperhost.creeperlauncher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import net.covers1624.quack.gson.PathTypeAdapter;
import net.creeperhost.creeperlauncher.util.MiscUtils;
import net.creeperhost.creeperlauncher.util.ProxyUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class Settings
{
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Type settingsToken = new TypeToken<HashMap<String, String>>(){}.getType();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static HashMap<String, String> settings = new HashMap<>();

    public static void saveSettings() {
        Path json = Constants.BIN_LOCATION.resolve("settings.json");
        if(Files.notExists(Constants.BIN_LOCATION))
        {
            try {
                Files.createDirectories(Constants.BIN_LOCATION);
            } catch(Exception ignored) {
                LOGGER.error("Failed create directory [{}] for the settings.json", Constants.BIN_LOCATION);
            }
        }
        try (BufferedWriter writer = Files.newBufferedWriter(json, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE))
        {
            gson.toJson(settings, settingsToken, writer);
        }
        catch (IOException ignored) {
            LOGGER.error("Failed to write json data to settings.json");
        }
    }

    public static void loadSettings() {
        loadSettings(Constants.BIN_LOCATION.resolve("settings.json"), true);
        ProxyUtils.loadProxy();
    }

    // Only use directly during migrate logic to avoid saving settings immediately
    public static void loadSettings(Path json, boolean save)
    {
        try
        {

            if (Files.exists(json)) {
                try (BufferedReader reader = Files.newBufferedReader(json)) {
                    Settings.settings = gson.fromJson(reader, settingsToken);
                }
                if (Settings.settings.getClass() != HashMap.class)
                {
                    Settings.settings = new HashMap<>();
                }
            } else {
                Settings.settings = new HashMap<>();
            }
            Settings.settings.put("instanceLocation", Settings.settings.getOrDefault("instanceLocation", Constants.INSTANCES_FOLDER_LOC.toAbsolutePath().toString()));
            if (save)
            {
                saveSettings();
            }
        } catch (Exception err)
        {
            Settings.settings = new HashMap<>();
            Settings.settings.put("instanceLocation", Settings.settings.getOrDefault("instanceLocation", Constants.INSTANCES_FOLDER_LOC.toAbsolutePath().toString()));
            if (save)
            {
                saveSettings();
            }
        }
    }

    public static Path getPathOpt(String opt, Path default_) {
        String value = settings.get(opt);
        if (StringUtils.isEmpty(value)) {
            return default_;
        }
        return Paths.get(value);
    }

    public static Path getInstancesDir() {
        return getPathOpt("instanceLocation", Constants.INSTANCES_FOLDER_LOC);
    }

    public static boolean getBooleanOr(String name, boolean default_) {
        String value = settings.get(name);

        return name != null ? Boolean.parseBoolean(value) : default_;
    }

    public static String getDefaultThreadLimit(String arg)
    {
        int defaultThreads = (Runtime.getRuntime().availableProcessors() / 2) - 1;
        if(defaultThreads < 2) defaultThreads = 2;
        return String.valueOf(defaultThreads);
    }

    public static long getSpeedLimit() {
        String val = settings.putIfAbsent("speedLimit", "0");
        if (val == null) {
            return 0;
        }
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    public static int getThreadLimit() {
        return Integer.parseInt(Settings.settings.computeIfAbsent("threadLimit", Settings::getDefaultThreadLimit));
    }
    
    private static final String DEFAULT_SPEC = "1.0.0";
    
    private static final SettingsData DEFAULT_SETTINGS = new SettingsData(
        DEFAULT_SPEC,
        Constants.INSTANCES_FOLDER_LOC,
        new GeneralSettings(
            "release",
            5184000, // 60 days
            false,
            false
        ),
        new InstanceSettings(
            854,
            480,
            4096,
            false,
            "release",
            new HashMap<>(),
            new HashMap<>()
        ),
        new AppearanceSettings(
            false,
            true
        ),
        new ProxySettings(
            "NONE",
            "",
            0,
            "",
            ""
        ),
        new DownloadSettings(
            2,
            0
        )
    );

    /**
     * Load the settings from the json file but when ever a field is missing, take it from the default settings.
     */
    private static void loadSettingsSafely() {
        // Serialize the default settings to json
        String defaultSettingsJson = gson.toJson(DEFAULT_SETTINGS);
        
        // Load the settings from the json file
//        SettingsData settings = GsonUtils.loadJson(Constants.getDataDir().resolve("settings.json"), SettingsData.class);
    }
    
    private static class SettingsMigrator {        
        private static final Function<String, Map<String, String>> CONVERT_ARGS_LIST = (value) -> {
            var commands = MiscUtils.splitCommand(value);
            var map = new HashMap<String, String>();
            
            for (int i = 0; i < commands.size(); i++) {
                String command = commands.get(i);
                if (command.startsWith("-")) {
                    String key = command.substring(1);
                    String val = commands.get(i + 1);
                    
                    // Skip the next value if it's a flag
                    if (val.startsWith("-")) {
                        val = "";
                    }
                    
                    map.put(key, val);
                }
            }
            
            return map;
        };
        
        private final Map<String, String> oldSettings;
        
        public SettingsMigrator(Map<String, String> oldSettings) {
            this.oldSettings = oldSettings;
        }

        /**
         * Migrate old settings to the new format.
         */
        private SettingsData migrate() {
            return new SettingsData(
                DEFAULT_SPEC,
                getOrDefault("instanceLocation", Path::of, DEFAULT_SETTINGS.instanceLocation),
                new GeneralSettings(
                    getOrDefault("updateChannel", DEFAULT_SETTINGS.general.releaseChannel),
                    getOrDefault("cacheLife", Integer::parseInt, DEFAULT_SETTINGS.general.cacheLife),
                    getOrDefault("exitOverwolf", Boolean::parseBoolean, DEFAULT_SETTINGS.general.exitOverwolf),
                    getOrDefault("verbose", Boolean::parseBoolean, DEFAULT_SETTINGS.general.verbose)
                ),
                new InstanceSettings(
                    getOrDefault("width", Integer::parseInt, DEFAULT_SETTINGS.instanceDefaults.width),
                    getOrDefault("height", Integer::parseInt, DEFAULT_SETTINGS.instanceDefaults.height),
                    getOrDefault("memory", Integer::parseInt, DEFAULT_SETTINGS.instanceDefaults.memory),
                    getOrDefault("fullscreen", Boolean::parseBoolean, DEFAULT_SETTINGS.instanceDefaults.fullscreen),
                    getOrDefault("updateChannel", DEFAULT_SETTINGS.instanceDefaults.updateChannel),
                    getOrDefault("jvmargs", CONVERT_ARGS_LIST, DEFAULT_SETTINGS.instanceDefaults.javaArgs),
                    getOrDefault("shellArgs", CONVERT_ARGS_LIST, DEFAULT_SETTINGS.instanceDefaults.shellArgs)
                ),
                new AppearanceSettings(
                    getOrDefault("useSystemWindowStyle", Boolean::parseBoolean, DEFAULT_SETTINGS.appearance.useSystemWindowStyle),
                    getOrDefault("showAdverts", Boolean::parseBoolean, DEFAULT_SETTINGS.appearance.showAds)
                ),
                new ProxySettings(
                    getOrDefault("proxyType", DEFAULT_SETTINGS.proxy.type),
                    getOrDefault("proxyHost", DEFAULT_SETTINGS.proxy.host),
                    getOrDefault("proxyPort", Integer::parseInt, DEFAULT_SETTINGS.proxy.port),
                    getOrDefault("proxyUser", DEFAULT_SETTINGS.proxy.username),
                    getOrDefault("proxyPassword", DEFAULT_SETTINGS.proxy.password)
                ),
                new DownloadSettings(
                    getOrDefault("threadLimit", Integer::parseInt, DEFAULT_SETTINGS.download.threadLimit),
                    getOrDefault("speedLimit", Long::parseLong, DEFAULT_SETTINGS.download.speedLimit)
                )
            );
        }
        
        private <T> T getOrDefault(String key, Function<String, T> transform, T defaultValue) {
            String value = this.oldSettings.get(key);
            if (value == null) {
                return defaultValue;
            }
            
            return transform.apply(value);
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

    record SettingsData(
        String spec,
        @JsonAdapter(PathTypeAdapter.class) Path instanceLocation,
        GeneralSettings general,
        InstanceSettings instanceDefaults,
        AppearanceSettings appearance,
        ProxySettings proxy,
        DownloadSettings download
    ) {}

    record InstanceSettings(
        int width,
        int height,
        int memory,
        boolean fullscreen,
        String updateChannel,
        Map<String, String> javaArgs,
        Map<String, String> shellArgs
    ) {}

    record ProxySettings(
        String type, // Use correct enum
        String host,
        int port,
        String username,
        String password
    ) {}

    record AppearanceSettings(
        boolean useSystemWindowStyle,
        boolean showAds
    ) {}

    record GeneralSettings(
        String releaseChannel,
        int cacheLife,
        boolean exitOverwolf,
        boolean verbose
    ) {}

    record DownloadSettings(
        int threadLimit,
        long speedLimit
    ) {}
}
