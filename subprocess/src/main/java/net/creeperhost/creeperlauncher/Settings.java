package net.creeperhost.creeperlauncher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.creeperhost.creeperlauncher.api.WebSocketAPI;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;

public class Settings
{
    private static final Logger LOGGER = LogManager.getLogger("Settings");

    private static final Type settingsToken = new TypeToken<HashMap<String, String>>(){}.getType();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * We used to store the settings in the bin folder that'll be removed in the future. This is the odd location
     * that we'll need to ensure that we've migrated over to the new location.
     */
    private static final Path LEGACY_SETTINGS_PATH = Constants.BIN_LOCATION.resolve("settings.json");
    private static boolean legacySettingsMigrated = false;

    private static final Path SETTINGS_PATH = Constants.getDataDir().resolve("app_settings.json");

    public static HashMap<String, String> settings = new HashMap<>();
    public static WebSocketAPI webSocketAPI;

    public static void saveSettings()
    {
        // Unlikely but worth checking
        if(Files.notExists(Constants.getDataDir()))
        {
            try {
                Files.createDirectories(Constants.getDataDir());
            } catch(Exception ignored) {
                LOGGER.error("Failed create directory [{}] for the app_settings.json", Constants.getDataDir());
            }
        }

        // Save the settings
        try (BufferedWriter writer = Files.newBufferedWriter(SETTINGS_PATH, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE))
        {
            gson.toJson(settings, settingsToken, writer);
        }
        catch (IOException ignored) {
            LOGGER.error("Failed to write json data to app_settings.json");
        }
    }

    public static void loadSettings()
    {
        loadSettings(SETTINGS_PATH, true);
    }

    // Only use directly during migrate logic to avoid saving settings immediately
    private static void loadSettings(Path json, boolean save)
    {
        // First, before anything else, run a basic migration routine
        if (oldSettingsJsonExits(LEGACY_SETTINGS_PATH)) {
            migrateSettings();
        }

        try {
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

    /**
     * Figures out if the old settings json exists. If the file does not exist then cache that fact. Then quick return
     * if the cached value. If the file does exist then return true
     *
     * @return true if the old settings.json exists, false otherwise
     */
    private static boolean oldSettingsJsonExits(Path settingsPath) {
        if (legacySettingsMigrated) {
            return false;
        }

        boolean exists = Files.exists(settingsPath);
        if (!exists) {
            legacySettingsMigrated = true;
            return false;
        }

        return true;
    }

    /**
     * Migrates the settings.json file to the new location.
     */
    private static void migrateSettings() {
        try {
            if (Files.exists(LEGACY_SETTINGS_PATH)) {
                Path move = Files.move(LEGACY_SETTINGS_PATH, SETTINGS_PATH);
                if (Files.exists(move)) {
                    LOGGER.info("Migrated settings.json to new location (.ftba/app_settings.json)");
                    legacySettingsMigrated = true;
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to migrate settings.json to new location (.ftba/app_settings.json)", e);
        }
    }

    public static Path getPathOpt(String opt, Path default_) {
        String value = settings.get(opt);
        if (StringUtils.isEmpty(value)) {
            return default_;
        }
        return Paths.get(value);
    }

    public static Path getInstanceLocOr(Path default_) {
        return getPathOpt("instanceLocation", default_);
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

    public static Path getSettingsPath() {
        return SETTINGS_PATH;
    }
}
