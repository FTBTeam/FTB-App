package net.creeperhost.creeperlauncher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.creeperhost.creeperlauncher.api.WebSocketAPI;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;

public class Settings
{
    private static final Type settingsToken = new TypeToken<HashMap<String, String>>(){}.getType();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static HashMap<String, String> settings = new HashMap<>();
    public static WebSocketAPI webSocketAPI;

    public static void saveSettings()
    {
        Path json = Constants.BIN_LOCATION.resolve("settings.json");
        if(Files.notExists(Constants.BIN_LOCATION))
        {
            try {
                Files.createDirectories(Constants.BIN_LOCATION);
            } catch(Exception ignored) {}
        }
        try (BufferedWriter writer = Files.newBufferedWriter(json, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE))
        {
            gson.toJson(settings, settingsToken, writer);
        }
        catch (IOException ignored) {}
    }

    public static void loadSettings()
    {
        loadSettings(Constants.BIN_LOCATION.resolve("settings.json"), true);
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

    public static Path getInstanceLocOr(Path default_) {
        return getPathOpt("instanceLocation", default_);
    }

    public static String getDefaultThreadLimit(String arg)
    {
        int defaultThreads = (Runtime.getRuntime().availableProcessors() / 2) - 1;
        if(defaultThreads < 2) defaultThreads = 2;
        return String.valueOf(defaultThreads);
    }
}
