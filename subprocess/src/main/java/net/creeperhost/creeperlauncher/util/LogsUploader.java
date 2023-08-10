package net.creeperhost.creeperlauncher.util;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.covers1624.quack.platform.OperatingSystem;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.data.InstanceJson;
import net.creeperhost.creeperlauncher.os.OS;
import net.creeperhost.creeperlauncher.pack.Instance;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractOutputStreamAppender;
import org.jetbrains.annotations.Nullable;

import javax.annotation.RegEx;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Created by covers1624 on 24/2/21.
 */
public class LogsUploader {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();
    
    /**
     * Uploads the UI and Backend Debug logs to pste.ch
     *
     * @param uiVersion    The UI Version.

     * @return The pste.ch code, or null if an error occurred.
     */
    @Nullable
    public static String uploadUILogs(@Nullable String uiVersion) {
        var logs = collectLogs();

        var obj = new JsonObject();
        obj.addProperty("uiVersion", uiVersion);
        obj.addProperty("appVersion", Constants.APPVERSION);
        obj.addProperty("platform", Constants.PLATFORM);
        obj.addProperty("os", OS.CURRENT.toString());
        obj.addProperty("arch", System.getProperty("os.arch"));
        obj.addProperty("javaVersion", System.getProperty("java.version"));
        obj.addProperty("javaVendor", System.getProperty("java.vendor"));
        
        var logObj = new JsonObject();
        logs.forEach(logObj::addProperty);
        obj.add("logs", logObj);
        
        return uploadPaste(GSON.toJson(obj));
    }

    private static Map<String, String> collectLogs() {
        Map<String, String> logs = Maps.newHashMap();
        logs.put("debug.log", uploadIfNotEmpty(getDebugLog()));
        logs.put("settings.json", uploadIfNotEmpty(getSettings()));
        
        var frontendLogs = getFrontendLogs();
        frontendLogs.forEach((key, value) -> logs.put(key, uploadIfNotEmpty(value)));
        
        logs.put("versions.log", uploadIfNotEmpty(getVersions()));
        logs.put("runtimes.json", uploadIfNotEmpty(getRuntimes()));
        logs.put("instances.log", uploadIfNotEmpty(getInstances()));
        logs.put("instances-memory.json", uploadIfNotEmpty(getInstancesFromMemory()));
        
        return logs;
    }
    
    @Nullable
    private static String uploadIfNotEmpty(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        
        return uploadPaste(data);
    } 
    
    /**
     * Reads the latest backend debug.log file from disk.
     *
     * @return The string content, or null if an error occurred.
     */
    @Nullable
    public static String getDebugLog() {
        try {
            LoggerContext context = (LoggerContext) LogManager.getContext();
            for (org.apache.logging.log4j.core.Logger logger : context.getLoggers()) {
                for (Appender appender : logger.getAppenders().values()) {
                    if (appender instanceof AbstractOutputStreamAppender) {
                        ((AbstractOutputStreamAppender<?>) appender).getManager().flush();
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        Path debugLogFile = Constants.getDataDir().resolve("logs/debug.log");

        if (Files.exists(debugLogFile)) {
            try {
                return Files.readString(debugLogFile);
            } catch (IOException e) {
                LOGGER.warn("Failed to load debug logs.", e);
            }
        }
        return null;
    }

    private static Map<String, String> getFrontendLogs() {
        var frontendLogs = new HashMap<String, String>();
        
        if (!OperatingSystem.current().isWindows()) {
            // Electron logs
            frontendLogs.put("ftb-app-frontend.log", readOrEmpty(Constants.getDataDir().resolve("logs/ftb-app-frontend.log")));
            frontendLogs.put("ftb-app-electron.log", readOrEmpty(Constants.getDataDir().resolve("logs/ftb-app-electron.log")));
        } else {
            // Get AppData/Local
            var appData = System.getenv("LOCALAPPDATA");
            
            // Overwolf logs
            var appLocation = Path.of(appData, "Overwolf/Log/Apps", "FTB App");
            if (Files.exists(appLocation)) {
                String[] logs = {"index.html.log", "background.html.log", "chat.html.log"};
                for (String log : logs) {
                    frontendLogs.put(log, readOrEmpty(appLocation.resolve(log)));
                }
            }
        }
        
        return frontendLogs;
    }
    
    private static String getRuntimes() {
        var runtimes = readOrEmpty(Constants.BIN_LOCATION.resolve("runtime/installations.json"));
        if (runtimes.isEmpty()) {
            return "";
        }
        
        // Parse it as json (This makes it pretty!)
        return GSON.toJson(GSON.fromJson(runtimes, JsonElement.class));
    }
    
    private static String getVersions() {
        return pathToString(Constants.BIN_LOCATION.resolve("versions"));
    }
    
    private static String getSettings() {
        var settings = Constants.BIN_LOCATION.resolve("settings.json");
        
        // Parse it as json
        try {
            JsonElement jsonData = GsonUtils.loadJson(settings, JsonElement.class);
            if (jsonData == null) {
                return "";
            }

            if (jsonData.isJsonObject() && jsonData.getAsJsonObject().has("sessionString")) {
                jsonData.getAsJsonObject().remove("sessionString");
            }

            return GSON.toJson(jsonData);
        } catch (Exception e) {
            LOGGER.warn("Failed to parse settings.json", e);
        }
        
        return "";
    }
    
    private static String getInstances() {
        return pathToString(Constants.INSTANCES_FOLDER_LOC);
    }
    
    private static String getInstancesFromMemory() {
        return GSON.toJson(
            Instances.allInstances().stream()
                .map(e -> e.props)
                .map(e -> {
                    // Not lovely but it should work
                    var json = new InstanceJson(e);
                    json.art = "";
                    return json;
                })
                .toList()
        );
    }
    
    private static String pathToString(Path path) {
        // Ensure it's a directory and exists
        if (!Files.isDirectory(path) || !Files.exists(path)) return "";
        
        try (Stream<Path> files = Files.list(path)) {
            return files.toList()
                .stream()
                .map(Path::getFileName)
                .map(s -> s.toString() + "\n")
                .reduce(String::concat)
                .orElse("");
        } catch (IOException e) {
            LOGGER.warn("Failed to read {} directory", path, e);
        }
        
        return "";
    }
    
    private static String readOrEmpty(Path path) {
        if (!Files.exists(path)) return "";
        
        try {
            return Files.readString(path);
        } catch (IOException e) {
            LOGGER.warn("Failed to read file {}", path, e);
            return "";
        }
    }
    
    /**
     * Uploads the given data to pste.ch
     *
     * @param data The data to upload.
     * @return The pste.ch code, or null if an error occured.
     */
    @Nullable
    public static String uploadPaste(@Nullable String data) {
        String result = WebUtils.postWebResponse("https://pste.ch/documents", data == null ? "No data available." : data, "text/plain; charset=UTF-8");
        try {
            JsonObject objResponse = GsonUtils.GSON.fromJson(result, JsonObject.class);
            JsonElement key = objResponse.get("key");
            if (key == null || !key.isJsonPrimitive()) return null;

            return key.getAsString();
        } catch (Throwable e) {
            return null;
        }
    }

    private static String padString(String stringToPad) {
        int desiredLength = 86;
        char padChar = '=';
        int strLen = stringToPad.length();
        float halfLength = ((float) desiredLength - (float) strLen) / (float) 2;
        int leftPad;
        int rightPad;
        if (((int) halfLength) != halfLength) {
            leftPad = (int) halfLength + 1;
            rightPad = (int) halfLength;
        } else {
            leftPad = rightPad = (int) halfLength;
        }

        String padCharStr = String.valueOf(padChar);

        return padCharStr.repeat(leftPad).concat(stringToPad).concat(padCharStr.repeat(rightPad));
    }
}
