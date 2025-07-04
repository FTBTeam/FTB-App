package dev.ftb.app.storage.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.annotations.JsonAdapter;
import net.covers1624.quack.gson.PathTypeAdapter;
import dev.ftb.app.Constants;
import org.jetbrains.annotations.Nullable;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static dev.ftb.app.Constants.MOJANG_DEFAULT_ARGS;

/**
 * Damn settings need to be mutated so this can't be a record :cry:
 */
public class SettingsData {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    private String spec;
    @JsonAdapter(PathTypeAdapter.class)
    private Path instanceLocation;
    private Boolean enableFeralGameMode;
    private GeneralSettings general;
    private InstanceSettings instanceDefaults;
    private AppearanceSettings appearance;
    private ProxySettings proxy;
    private DownloadSettings download;
    private WorkaroundSettings workaround;

    public SettingsData(String spec, Path instanceLocation, Boolean enableFeralGameMode, GeneralSettings general, InstanceSettings instanceDefaults, AppearanceSettings appearance, ProxySettings proxy, DownloadSettings download, WorkaroundSettings workaround) {
        this.spec = spec;
        this.instanceLocation = instanceLocation;
        this.enableFeralGameMode = enableFeralGameMode;
        this.general = general;
        this.instanceDefaults = instanceDefaults;
        this.appearance = appearance;
        this.proxy = proxy;
        this.download = download;
        this.workaround = workaround;
    }

    // Writer
    public void write() {
        var jsonData = gson.toJson(this);
        try {
            if (Files.notExists(Constants.SETTINGS_FILE.getParent())) {
                Files.createDirectories(Constants.SETTINGS_FILE.getParent());
            }
            
            Files.writeString(Constants.SETTINGS_FILE, jsonData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Reader
    public static SettingsData read() {
        try {
            var jsonData = Files.readString(Constants.SETTINGS_FILE);
            if (jsonData.isBlank()) {
                throw new IOException("Settings file is empty.");
            }
            return gson.fromJson(jsonData, SettingsData.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static SettingsData fromString(String json) {
        return gson.fromJson(json, SettingsData.class);
    }

    @Override
    public String toString() {
        return gson.toJson(this);
    }

    public JsonObject toJson() {
        return gson.toJsonTree(this).getAsJsonObject();
    }
    
    public static SettingsData createDefault() {
        return new SettingsData(
            Settings.DEFAULT_SPEC,
            Constants.INSTANCES_FOLDER_LOC,
            false, // Feral Game Mode is disabled by default
            new GeneralSettings(
                "release",
                5184000, // 60 days
                false,
                false
            ),
            new InstanceSettings(
                getScreenWidth(),
                getScreenHeight(),
                computeRecommendedInstanceRam(),
                false,
                "release",
                false,
                String.join(" ", MOJANG_DEFAULT_ARGS), 
                "",
                ""
            ),
            new AppearanceSettings(
                false,
                true
            ),
            new ProxySettings(
                "NONE",
                "",
                -1,
                "",
                ""
            ),
            new DownloadSettings(
                Settings.getDefaultThreadLimit(),
                0
            ),
            new WorkaroundSettings(
                false
            )
        );
    }
    
    private static int getScreenWidth() {
        if (System.getenv("CI") != null) return 1920; // CI doesn't have a screen size
        try {
            return (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
        } catch (Exception e) {
            // Fallback for environments where Toolkit is not available
            return 1920; // Default width
        }
    }
    
    private static int getScreenHeight() {
        if (System.getenv("CI") != null) return 1080; // CI doesn't have a screen size
        try {
            return (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight();
        } catch (Exception e) {
            // Fallback for environments where Toolkit is not available
            return 1080; // Default height
        }
    }

    public String spec() {
        return spec;
    }

    public Path instanceLocation() {
        return instanceLocation;
    }
    
    @Nullable
    public Boolean enableFeralGameMode() {
        return enableFeralGameMode;
    }

    public GeneralSettings general() {
        return general;
    }

    public InstanceSettings instanceDefaults() {
        return instanceDefaults;
    }

    public AppearanceSettings appearance() {
        return appearance;
    }

    public ProxySettings proxy() {
        return proxy;
    }

    public DownloadSettings download() {
        return download;
    }

    public WorkaroundSettings workaround() {
        return workaround;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public void setInstanceLocation(Path instanceLocation) {
        this.instanceLocation = instanceLocation;
    }
    
    public void setEnableFeralGameMode(Boolean enableFeralGameMode) {this.enableFeralGameMode = enableFeralGameMode;}

    public void setGeneral(GeneralSettings general) {
        this.general = general;
    }

    public void setInstanceDefaults(InstanceSettings instanceDefaults) {
        this.instanceDefaults = instanceDefaults;
    }

    public void setAppearance(AppearanceSettings appearance) {
        this.appearance = appearance;
    }

    public void setProxy(ProxySettings proxy) {
        this.proxy = proxy;
    }

    public void setDownload(DownloadSettings download) {
        this.download = download;
    }

    public static final class InstanceSettings {
        private int width;
        private int height;
        private int memory;
        private boolean fullscreen;
        private String updateChannel;
        private boolean preventMetaModInjection;
        private String javaArgs;
        private String shellArgs;
        private String programArgs;

        public InstanceSettings(int width, int height, int memory, boolean fullscreen, String updateChannel, boolean preventMetaModInjection, String javaArgs, String shellArgs, String programArgs) {
            this.width = width;
            this.height = height;
            this.memory = memory;
            this.fullscreen = fullscreen;
            this.updateChannel = updateChannel;
            this.preventMetaModInjection = preventMetaModInjection;
            this.javaArgs = javaArgs;
            this.shellArgs = shellArgs;
            this.programArgs = programArgs;
        }

        public int width() {
            return width;
        }

        public int height() {
            return height;
        }

        public int memory() {
            return memory;
        }

        public boolean fullscreen() {
            return fullscreen;
        }

        public String updateChannel() {
            return updateChannel;
        }
        
        public boolean preventMetaModInjection() {
            return preventMetaModInjection;
        }

        public String javaArgs() {
            return javaArgs;
        }

        public String shellArgs() {
            return shellArgs;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public void setMemory(int memory) {
            this.memory = memory;
        }

        public void setFullscreen(boolean fullscreen) {
            this.fullscreen = fullscreen;
        }

        public void setUpdateChannel(String updateChannel) {
            this.updateChannel = updateChannel;
        }

        public void setJavaArgs(String javaArgs) {
            this.javaArgs = javaArgs;
        }

        public void setShellArgs(String shellArgs) {
            this.shellArgs = shellArgs;
        }

        public String getProgramArgs() {
            return programArgs;
        }

        public void setProgramArgs(String programArgs) {
            this.programArgs = programArgs;
        }
    }

    public static final class ProxySettings {
        private String type;
        private String host;
        private int port;
        private String username;
        private String password;

        public ProxySettings(String type, String host, int port, String username, String password) {
            this.type = type;
            this.host = host;
            this.port = port;
            this.username = username;
            this.password = password;
        }

        public String type() {
            return type;
        }

        public String host() {
            return host;
        }

        public int port() {
            return port;
        }

        public String username() {
            return username;
        }

        public String password() {
            return password;
        }

        public void setType(String type) {
            this.type = type;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static final class AppearanceSettings {
        private boolean useSystemWindowStyle;
        private boolean showAds;

        public AppearanceSettings(boolean useSystemWindowStyle, boolean showAds) {
            this.useSystemWindowStyle = useSystemWindowStyle;
            this.showAds = showAds;
        }

        public boolean useSystemWindowStyle() {
            return useSystemWindowStyle;
        }

        public boolean showAds() {
            return showAds;
        }

        public void setUseSystemWindowStyle(boolean useSystemWindowStyle) {
            this.useSystemWindowStyle = useSystemWindowStyle;
        }

        public void setShowAds(boolean showAds) {
            this.showAds = showAds;
        }
    }

    public static final class GeneralSettings {
        private final String releaseChannel;
        private final int cacheLife;
        private final boolean exitOverwolf;
        private final boolean verbose;

        public GeneralSettings(
            String releaseChannel,
            int cacheLife,
            boolean exitOverwolf,
            boolean verbose
        ) {
            this.releaseChannel = releaseChannel;
            this.cacheLife = cacheLife;
            this.exitOverwolf = exitOverwolf;
            this.verbose = verbose;
        }

        public String releaseChannel() {
            return releaseChannel;
        }

        public int cacheLife() {
            return cacheLife;
        }

        public boolean exitOverwolf() {
            return exitOverwolf;
        }

        public boolean verbose() {
            return verbose;
        }
    }

    public static final class DownloadSettings {
        private int threadLimit;
        private long speedLimit;

        public DownloadSettings(int threadLimit, long speedLimit) {
            this.threadLimit = threadLimit;
            this.speedLimit = speedLimit;
        }

        public int threadLimit() {
            return threadLimit;
        }

        public long speedLimit() {
            return speedLimit;
        }
    }

    public static final class WorkaroundSettings {

        public boolean ignoreForgeProcessorOutputHashes;

        public WorkaroundSettings(boolean ignoreForgeProcessorOutputHashes) {
            this.ignoreForgeProcessorOutputHashes = ignoreForgeProcessorOutputHashes;
        }

        public boolean ignoreForgeProcessorOutputHashes() {
            return ignoreForgeProcessorOutputHashes;
        }
    }
    
    private static int computeRecommendedInstanceRam() {
        HardwareAbstractionLayer hal = new SystemInfo().getHardware();
        long totalMemory = hal.getMemory().getTotal() / 1024 / 1024;
        
        // If the users ram is over 8GB we'll default to 6GB
        if (totalMemory > 8192) {
            return 6144;
        }
        
        // Otherwise default to 4GB
        return 4096;
    }
}
