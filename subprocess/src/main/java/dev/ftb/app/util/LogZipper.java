package dev.ftb.app.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.ftb.app.Constants;
import dev.ftb.app.Instances;
import dev.ftb.app.accounts.AccountManager;
import dev.ftb.app.data.InstanceJson;
import dev.ftb.app.os.OS;
import dev.ftb.app.pack.Instance;
import dev.ftb.app.storage.settings.Settings;
import dev.ftb.app.storage.settings.SettingsData;
import net.covers1624.quack.platform.OperatingSystem;
import net.covers1624.quack.util.LazyValue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractOutputStreamAppender;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.NetworkInterface;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class LogZipper {
    /**
     * Manifest version
     */
    private static final String VERSION = "3.0.0";

    private static final List<String> USER_HOME_POSSIBLE_ENVS = List.of("HOME", "USERPROFILE", "HOMEPATH", "HOMEDRIVE");
    
    private static final Pattern UUID_REMOVAL = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    private static final Pattern JWT_REMOVAL = Pattern.compile("e[yw][A-Za-z0-9-_]+\\.(?:e[yw][A-Za-z0-9-_]+)?\\.[A-Za-z0-9-_]{2,}(?:(?:\\.[A-Za-z0-9-_]{2,}){2})?");

    private static final List<Pattern> USER_NAME_REMOVAL = List.of(
        Pattern.compile("C:\\\\Users\\\\([^\\\\]+)\\\\"),
        Pattern.compile("C:/Users/([^/]+)/"),
        Pattern.compile("(?<!\\w)/home/[^/]+/"),
        Pattern.compile("(?<!\\w)/Users/[^/]+/"),
        Pattern.compile("^USERNAME=.+$", Pattern.MULTILINE)
    );

    private static final List<Pattern> ALL_REMOVALS = new ArrayList<>() {{
        add(UUID_REMOVAL);
        add(JWT_REMOVAL);
        addAll(USER_NAME_REMOVAL);
    }};

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    private final LazyValue<List<String>> ipAddresses = new LazyValue<>(this::getUserIpAddresses);

    private final long startTime;

    private LogZipper() {
        this.startTime = System.currentTimeMillis();
    }

    public static LogZipper create() {
        return new LogZipper();
    }

    public Path generate() {
        var obj = new JsonObject();

        obj.addProperty("version", VERSION);

        var metaDetails = new JsonObject();
        metaDetails.addProperty("instanceCount", Instances.allInstances().size());
        metaDetails.addProperty("today", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
        metaDetails.addProperty("time", System.currentTimeMillis());
        metaDetails.addProperty("timeTaken", System.currentTimeMillis() - startTime);
        metaDetails.addProperty("addedAccounts", AccountManager.get().getProfiles().size());
        metaDetails.addProperty("hasActiveAccount", AccountManager.get().getActiveProfile() != null);
        obj.add("metaDetails", metaDetails);

        var appDetails = new JsonObject();
        appDetails.addProperty("version", Constants.APPVERSION);
        appDetails.addProperty("platform", Constants.PLATFORM);
        obj.add("appDetails", appDetails);

        var systemDetails = new JsonObject();
        systemDetails.addProperty("os", OS.CURRENT.toString());
        systemDetails.addProperty("arch", System.getProperty("os.arch"));
        systemDetails.addProperty("javaVersion", System.getProperty("java.version"));
        systemDetails.addProperty("javaVendor", System.getProperty("java.vendor"));
        obj.add("appDetails", appDetails);

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

        var jsonData = GSON.toJson(obj);
        var userHome = locateUserHome();
        Path userDownloadsFolder = null;
        
        if (userHome != null && Files.exists(userHome.resolve("Downloads"))) {
            userDownloadsFolder = userHome.resolve("Downloads");
        } else if (userHome != null && Files.exists(userHome.resolve("Desktop"))) {
            userDownloadsFolder = userHome.resolve("Desktop");
        } else {
            LOGGER.warn("User downloads folder does not exist, using data dir instead");
            userDownloadsFolder = Constants.getDataDir();
        }
        
        if (Files.notExists(userDownloadsFolder)) {
            throw new RuntimeException("User output path not resolvable");
        }

        var today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
        var outFile = userDownloadsFolder.resolve("ftb-app-logs-" + today + ".zip");

        try (
            var fileOutput = Files.newOutputStream(outFile);
            var writer = new ZipOutputStream(fileOutput)) {

            addFileToZip(writer, "manifest.json", jsonData, true);

            // Put all the logs into the zip as well
            includeAppData(writer);
            includeLastThreeDebugLogsFromHistory(writer);
            includeInstanceLogs(writer);

            for (var log : getFrontendLogs()) {
                try {
                    addFileToZipFromPath(writer, "app-data/" + log.getFileName().toString(), log);
                } catch (IOException e) {
                    LOGGER.warn("Failed to write log {}", log, e);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to write logs to zip", e);
            
            try {
                LOGGER.warn("Deleting the file {} as the export failed", outFile);
                Files.deleteIfExists(outFile);
            } catch (IOException ex) {
                LOGGER.error("Failed to delete the file", ex);
            }
            
            throw new RuntimeException(e);
        }

        return outFile;
    }

    private String getFilteredSettings() {
        try {
            SettingsData settings = Settings.getSettings();
            var proxySettingsOld = settings.proxy();
            // References...
            settings.setProxy(null);
            var output = GSON.toJson(settings);
            settings.setProxy(proxySettingsOld);
            return output;
        } catch (Throwable e) {
            LOGGER.warn("Failed to get settings", e);
            return "settings failed to load";
        }
    }

    private void includeAppData(ZipOutputStream writer) {
        var logs = Map.of(
            "debug.log", getDebugLog(),
            "versions.log", getVersions(),
            "runtimes.json", getRuntimes(),
            "instances.log", getInstances(),
            "instances-memory.json", getInstancesFromMemory(),
            "settings.json", getFilteredSettings()
        );

        for (Map.Entry<String, String> entry : logs.entrySet()) {
            try {
                addFileToZip(writer, "app-data/" + entry.getKey(), entry.getValue(), entry.getKey().equals("instances-memory.json"));
            } catch (IOException e) {
                LOGGER.warn("Failed to write log {}", entry.getKey(), e);
            }
        }
    }

    private void includeInstanceLogs(ZipOutputStream writer) throws IOException {
        var instances = Instances.allInstances();
        if (instances.isEmpty()) {
            return;
        }

        for (Instance instance : instances) {
            var instanceObj = new JsonObject();

            var packPathName = Constants.INSTANCES_FOLDER_LOC.relativize(instance.getDir()).toString();

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

            for (var crashlog : crashLogs) {
                addFileToZipFromPath(writer, "instance/" + packPathName + "/" + crashlog.getFileName().toString(), crashlog);
            }

            var logFiles = List.of("debug.log", "console.log", "latest.log");
            for (var logFile : logFiles) {
                addFileToZipFromPath(writer, "instance/" + packPathName + "/" + logFile, instance.getDir().resolve("logs/" + logFile));
            }

            // Write the json file to the instance folder
            try {
                addFileToZip(writer, "instance/" + packPathName + "/instance.json", GSON.toJson(instanceObj), true);
            } catch (IOException e) {
                LOGGER.warn("Failed to write instance.json", e);
            }
        }
    }

    /**
     * Reads the latest backend debug.log file from disk.
     *
     * @return The string content, or null if an error occurred.
     */
    public String getDebugLog() {
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

        return "no-contents";
    }

    public void includeLastThreeDebugLogsFromHistory(ZipOutputStream writer) {
        var files = List.of("debug-1.log.gz", "debug-2.log.gz", "debug-3.log.gz");
        var existingFiles = files.stream()
            .map(Constants.getDataDir().resolve("logs")::resolve)
            .filter(Files::exists)
            .toList();

        if (existingFiles.isEmpty()) {
            return;
        }

        // Map the file content to the file name
        for (Path file : existingFiles) {
            try (FileInputStream fis = new FileInputStream(file.toFile())) {
                GZIPInputStream gis = new GZIPInputStream(fis);

                // Read the file to a string
                addFileToZip(writer, "app-data/" + file.getFileName().toString().replace(".gz", ""), new String(gis.readAllBytes()));
            } catch (IOException e) {
                LOGGER.warn("Failed to read debug log file {}", file, e);
            }
        }
    }

    private List<Path> getFrontendLogs() {
        List<Path> appLogs = new ArrayList<>();

        if (!OperatingSystem.current().isWindows()) {
            // Electron logs
            appLogs.add(Constants.getDataDir().resolve("logs/ftb-app-electron.log"));
            appLogs.add(Constants.getDataDir().resolve("logs/ftb-app-frontend.log"));
        } else {
            // Get AppData/Local
            var appData = System.getenv("LOCALAPPDATA");

            // Overwolf logs
            var appLocation = Path.of(appData, "Overwolf/Log/Apps", "FTB App");
            if (Files.exists(appLocation)) {
                String[] logs = {"index.html.log", "background.html.log", "chat.html.log"};
                for (String log : logs) {
                    appLogs.add(appLocation.resolve(log));
                }
            }
        }

        return appLogs;
    }

    private String getRuntimes() {
        var runtimesPath = Constants.BIN_LOCATION.resolve("runtime/installations.json");
        if (Files.notExists(runtimesPath)) {
            return "";
        }

        String contents;
        try {
            contents = Files.readString(runtimesPath);
            return GSON.toJson(GSON.fromJson(contents, JsonElement.class));
        } catch (IOException e) {
            return "";
        }
    }

    private String getVersions() {
        return getTopLevelDirectoriesAsString(Constants.BIN_LOCATION.resolve("versions"));
    }

    private String getInstances() {
        return getTopLevelDirectoriesAsString(Constants.INSTANCES_FOLDER_LOC);
    }

    private String getInstancesFromMemory() {
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

    private String getTopLevelDirectoriesAsString(Path path) {
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

    private void addFileToZipFromPath(ZipOutputStream stream, String name, Path path) throws IOException {
        addFileToZipFromPath(stream, name, path, false);
    }

    private void addFileToZipFromPath(ZipOutputStream stream, String name, Path path, boolean skipFilter) throws IOException {
        if (Files.notExists(path) || Files.isDirectory(path)) {
            return;
        }

        var contents = Files.readString(path);
        addFileToZip(stream, name, contents, skipFilter);
    }

    private void addFileToZip(ZipOutputStream stream, String name, String contents) throws IOException {
        addFileToZip(stream, name, contents, false);
    }

    private void addFileToZip(ZipOutputStream stream, String name, String contents, boolean skipFilter) throws IOException {
        if (contents == null || contents.isEmpty()) {
            return;
        }

        var finalContents = skipFilter ? contents : filterContents(contents);

        stream.putNextEntry(new ZipEntry(name));
        stream.write(finalContents.getBytes());
        stream.closeEntry();
    }

    private String filterContents(String contents) {
        var addresses = ipAddresses.get();
        for (String address : addresses) {
            if (address.equals("127.0.0.1") || address.equals("0:0:0:0:0:0:0:1") || address.equals("localhost")) {
                continue;
            }
            
            contents = contents.replace(address, "//REDACTED-IP//");
        }

        for (Pattern pattern : ALL_REMOVALS) {
            contents = pattern.matcher(contents).replaceAll("//REDACTED//");
        }

        return contents;
    }

    /**
     * Get the IP addresses so we can filter them out of the logs if needed.
     * We don't want to keep these in the logs.
     */
    private List<String> getUserIpAddresses() {
        List<String> ipAddresses = new ArrayList<>();

        try {
            var interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                var networkInterface = interfaces.nextElement();
                var addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    var address = addresses.nextElement();
                    var hostNameAddress = address.getHostAddress();
                    if (address.isAnyLocalAddress()) {
                        continue;
                    }

                    if (hostNameAddress.contains("%")) {
                        hostNameAddress = hostNameAddress.substring(0, hostNameAddress.indexOf('%'));
                    }

                    ipAddresses.add(hostNameAddress);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to get network interfaces", e);
        }

        return ipAddresses;
    }
    
    private static Path locateUserHome() {
        // Try the simplest route first
        var userHome = System.getProperty("user.home");
        var userHomePath = Path.of(userHome);
        if (Files.exists(userHomePath)) {
            return userHomePath;
        }
        
        // Try the environment variables
        for (String env : USER_HOME_POSSIBLE_ENVS) {
            var envPath = System.getenv(env);
            if (envPath != null) {
                var envPathResolved = Path.of(envPath);
                if (Files.exists(envPathResolved)) {
                    return envPathResolved;
                }
            }
        }
        
        return null;
    }
}
