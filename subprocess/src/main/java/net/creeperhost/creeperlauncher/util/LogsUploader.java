package net.creeperhost.creeperlauncher.util;

import com.google.common.collect.Maps;
import com.google.gson.*;
import net.covers1624.quack.platform.OperatingSystem;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.accounts.AccountManager;
import net.creeperhost.creeperlauncher.data.InstanceJson;
import net.creeperhost.creeperlauncher.os.OS;
import net.creeperhost.creeperlauncher.pack.Instance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractOutputStreamAppender;
import org.jetbrains.annotations.Nullable;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

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
        var obj = new JsonObject();
        
        obj.addProperty("version", "2.0.0");
        
        var metaDetails = new JsonObject();
        metaDetails.addProperty("instanceCount", Instances.allInstances().size());
        metaDetails.addProperty("cloudInstances", Instances.allInstances().stream().filter(e -> e.props.cloudSaves).count());
        metaDetails.addProperty("today", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        metaDetails.addProperty("time", System.currentTimeMillis());
        metaDetails.addProperty("addedAccounts", AccountManager.get().getProfiles().size());
        metaDetails.addProperty("hasActiveAccount", AccountManager.get().getActiveProfile() != null);
        obj.add("metaDetails", metaDetails);
        
        var appDetails = new JsonObject();
        appDetails.addProperty("ui", uiVersion);
        appDetails.addProperty("app", Constants.APPVERSION);
        appDetails.addProperty("platform", Constants.PLATFORM);
        obj.add("appDetails", appDetails);
        
        var systemDetails = new JsonObject();
        systemDetails.addProperty("os", OS.CURRENT.toString());
        systemDetails.addProperty("arch", System.getProperty("os.arch"));
        systemDetails.addProperty("javaVersion", System.getProperty("java.version"));
        systemDetails.addProperty("javaVendor", System.getProperty("java.vendor"));
        obj.add("appDetails", appDetails);
        
        obj.add("appLogs", collectLogs());
        
        var providerInstanceMapping = new JsonObject();
        for (Instance instance : Instances.allInstances()) {
            var instanceObj = new JsonObject();
            instanceObj.addProperty("name", instance.getName());
            instanceObj.addProperty("packType", instance.props.packType);
            instanceObj.addProperty("packId", instance.props.id);
            instanceObj.addProperty("packVersion", instance.props.versionId);
            
            providerInstanceMapping.add(instance.getUuid().toString(), instanceObj);
        }
        obj.add("providerInstanceMapping", providerInstanceMapping);
        
        // Collect all the instance logs
        obj.add("instanceLogs", collectInstanceLogs());
        
        return uploadPaste(GSON.toJson(obj));
    }

    private static String getFilteredSettings() {
        var settingsData = new HashMap<>(Settings.settings);
        if (settingsData.containsKey("sessionString")) {
            settingsData.put("sessionStringWasEmpty", settingsData.get("sessionString").isEmpty() + "");
            settingsData.put("sessionString", "REDACTED");
        }
        
        return uploadIfNotEmpty(GSON.toJson(settingsData));
    }
    
    private static JsonObject collectLogs() {
        JsonObject logs = new JsonObject();
        logs.addProperty("debug.log", uploadIfNotEmpty(getDebugLog()));
        
        var frontendLogs = getFrontendLogs();
        frontendLogs.forEach((key, value) -> logs.addProperty(key, uploadIfNotEmpty(value)));
        
        combineAndUpload(Map.of(
            "versions.log", getVersions(),
            "runtimes.json", getRuntimes(),
            "instances.log", getInstances(),
            "instances-memory.json", getInstancesFromMemory(),
            "settings.json", getFilteredSettings()
        ))
            .entrySet()
            .forEach(e -> logs.addProperty(e.getKey(), e.getValue().getAsString()));
        
        getLastTwoDebugLogsFromHistory()
            .forEach((key, value) -> logs.addProperty(key, uploadIfNotEmpty(value)));
        
        return logs;
    }
    
    private static JsonArray collectInstanceLogs() {
        var instances = Instances.allInstances();
        if (instances.isEmpty()) {
            return new JsonArray();
        }
        
        var instanceLogs = new JsonArray();
        for (Instance instance : instances) {
            var instanceObj = new JsonObject();
            
            // Get the creation time of the instance dir
            var creationTime = 0L;
            try {
                creationTime = Files.getFileAttributeView(instance.getDir(), BasicFileAttributeView.class)
                    .readAttributes()
                    .creationTime()
                    .toInstant()
                    .getEpochSecond();
            } catch (IOException e) {
                LOGGER.warn("Failed to get creation time of instance {}", instance, e);
            }

            instanceObj.addProperty("created", creationTime != 0 ? creationTime : null);
            instanceObj.addProperty("name", instance.getName());
            instanceObj.addProperty("uuid", instance.getUuid().toString());
            instanceObj.addProperty("mcVersion", instance.getMcVersion());
            instanceObj.addProperty("modloader", instance.props.modLoader);
            
            var today = LocalDate.now();
            var todayFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            var crashLogs = FileUtils.listDir(instance.getDir().resolve("crash-reports"))
                .stream()
                .filter(e -> e.getFileName().toString().endsWith(".txt") 
                    && e.getFileName().toString().startsWith("crash-") 
                    && e.getFileName().toString().contains(today.format(todayFormat))
                ).toList();
            
            instanceObj.add("crashLogs", crashLogs.isEmpty() ? null : combineAndUploadPaths(crashLogs));
            
            var logs = new JsonObject();
            logs.addProperty("debug.log", uploadIfNotEmpty(readOrEmpty(instance.getDir().resolve("logs/debug.log"))));
            logs.addProperty("latest.log", uploadIfNotEmpty(readOrEmpty(instance.getDir().resolve("logs/latest.log"))));
            instanceObj.add("logs", logs);
            
            instanceLogs.add(instanceObj);
        }
        
        return instanceLogs;
    }
    
    @Nullable
    private static String uploadIfNotEmpty(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        
        return uploadPaste(data);
    }
    
    /**
     * Combines the given paths into a single string and uploads it to pste.ch
     * <p>
     * We use the following format:
     * uuid|base64 separated by newlines. Each line is a file. Each uuid maps to a file.
     */
    private static JsonObject combineAndUploadPaths(List<Path> paths) {
        var pathMapping = paths
            .stream()
            .collect(Collectors.toMap(
                e -> e.getFileName().toString(),
                LogsUploader::readOrEmpty
            ));
        
        return combineAndUpload(pathMapping);
    }
    
    private static JsonObject combineAndUpload(Map<String, String> nameToInputMapping) {
        var pathToUuidMapping = new HashMap<String, String>();
        var combined = new StringBuilder();
        for (Map.Entry<String, String> nameAndInput : nameToInputMapping.entrySet()) {
            var line = new StringBuilder();

            var uuid = UUID.randomUUID();
            var base64Input = Base64.getEncoder().encodeToString(nameAndInput.getValue().getBytes());

            line.append(uuid).append("|").append(base64Input);
            combined.append(line).append("\n");

            pathToUuidMapping.put(nameAndInput.getKey(), uuid.toString());
        }

        var code = uploadIfNotEmpty(combined.toString());
        if (code == null) {
            return new JsonObject();
        }

        var obj = new JsonObject();
        for (var entry : pathToUuidMapping.entrySet()) {
            obj.addProperty(entry.getKey(), code + "|" + entry.getValue());
        }

        return obj;
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
    
    public static Map<String, String> getLastTwoDebugLogsFromHistory() {
        var files = List.of("debug-1.log.gz", "debug-2.log.gz");
        var existingFiles = files.stream()
            .map(Constants.getDataDir().resolve("logs")::resolve)
            .filter(Files::exists)
            .toList();
        
        if (existingFiles.isEmpty()) {
            return Map.of();
        }
        
        // Map the file content to the file name
        Map<String, String> logs = Maps.newHashMap();
        for (Path file : existingFiles) {
            try (FileInputStream fis = new FileInputStream(file.toFile())) {
                GZIPInputStream gis = new GZIPInputStream(fis);
                
                // Read the file to a string
                String content = new String(gis.readAllBytes());
                logs.put(file.getFileName().toString(), content);
            } catch (IOException e) {
                LOGGER.warn("Failed to read debug log file {}", file, e);
            }
        }
        
        return logs;
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
}
