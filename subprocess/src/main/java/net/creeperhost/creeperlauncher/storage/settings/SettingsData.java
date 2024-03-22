package net.creeperhost.creeperlauncher.storage.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.annotations.JsonAdapter;
import net.covers1624.quack.gson.PathTypeAdapter;
import net.creeperhost.creeperlauncher.Constants;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Damn settings need to be mutated so this can't be a record :cry:
 */
public class SettingsData {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    private String spec;
    @JsonAdapter(PathTypeAdapter.class)
    private Path instanceLocation;
    private GeneralSettings general;
    private InstanceSettings instanceDefaults;
    private AppearanceSettings appearance;
    private ProxySettings proxy;
    private DownloadSettings download;

    public SettingsData(String spec, Path instanceLocation, GeneralSettings general, InstanceSettings instanceDefaults, AppearanceSettings appearance, ProxySettings proxy, DownloadSettings download) {
        this.spec = spec;
        this.instanceLocation = instanceLocation;
        this.general = general;
        this.instanceDefaults = instanceDefaults;
        this.appearance = appearance;
        this.proxy = proxy;
        this.download = download;
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
            new GeneralSettings(
                "release",
                5184000, // 60 days
                false,
                false
            ),
            new InstanceSettings(
                (int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 2),
                (int) (Toolkit.getDefaultToolkit().getScreenSize().getHeight() / 2),
                4096,
                false,
                "release",
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
            )
        );
    }

    public String spec() {
        return spec;
    }

    public Path instanceLocation() {
        return instanceLocation;
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

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public void setInstanceLocation(Path instanceLocation) {
        this.instanceLocation = instanceLocation;
    }

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
        private String javaArgs;
        private String shellArgs;

        public InstanceSettings(int width, int height, int memory, boolean fullscreen, String updateChannel, String javaArgs, String shellArgs) {
            this.width = width;
            this.height = height;
            this.memory = memory;
            this.fullscreen = fullscreen;
            this.updateChannel = updateChannel;
            this.javaArgs = javaArgs;
            this.shellArgs = shellArgs;
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
}
