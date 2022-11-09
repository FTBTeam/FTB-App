package net.creeperhost.creeperlauncher.data;

import com.google.gson.Gson;
import com.google.gson.annotations.JsonAdapter;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.gson.PathTypeAdapter;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.data.modpack.ModpackManifest;
import net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Created by covers1624 on 9/11/22.
 */
public class InstanceJson {

    private static final Gson GSON = new Gson();

    public UUID uuid;
    public long id;
    public long versionId;

    public String name;
    public String version;
    public String mcVersion;

    @Deprecated // May not be required, it's mirrored from the version manifest.
    public int minMemory = 2048;
    @Deprecated  // May not be required, it's mirrored from the version manifest.
    public int recMemory = 4096;
    public int memory = Integer.parseInt(Settings.settings.getOrDefault("memory", "2048"));

    public String jvmArgs = Settings.settings.getOrDefault("jvmArgs", "");
    public boolean embeddedJre = Boolean.parseBoolean(Settings.settings.getOrDefault("embeddedJre", "true"));
    ;
    @JsonAdapter (PathTypeAdapter.class)
    public Path jrePath = Settings.getPathOpt("jrePath", null);
    public int width = Integer.parseInt(Settings.settings.getOrDefault("width", String.valueOf((int) Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 2)));
    ;
    public int height = Integer.parseInt(Settings.settings.getOrDefault("height", String.valueOf((int) Toolkit.getDefaultToolkit().getScreenSize().getHeight() / 2)));
    public String modLoader = "";

    public boolean isModified;
    public boolean isImport;
    public boolean cloudSaves;
    public boolean hasInstMods;
    public boolean installComplete;

    public byte packType;
    // TODO migrate this to `isPrivate`
    public boolean _private;

    /**
     * The current play time in millis.
     */
    public long totalPlayTime;
    public long lastPlayed;

    // Gson
    InstanceJson() {
    }

    public InstanceJson(InstanceJson other) {
        this.uuid = other.uuid;
        this.id = other.id;
        this.versionId = other.versionId;
        this.name = other.name;
        this.version = other.version;
        this.mcVersion = other.mcVersion;
        this.minMemory = other.minMemory;
        this.recMemory = other.recMemory;
        this.memory = other.memory;
        this.jvmArgs = other.jvmArgs;
        this.embeddedJre = other.embeddedJre;
        this.jrePath = other.jrePath;
        this.width = other.width;
        this.height = other.height;
        this.modLoader = other.modLoader;
        this.isModified = other.isModified;
        this.isImport = other.isImport;
        this.cloudSaves = other.cloudSaves;
        this.hasInstMods = other.hasInstMods;
        this.installComplete = other.installComplete;
        this.packType = other.packType;
        this._private = other._private;
        this.totalPlayTime = other.totalPlayTime;
        this.lastPlayed = other.lastPlayed;
    }

    public InstanceJson(InstanceJson other, UUID uuid, String name) {
        this(other);
        this.uuid = uuid;
        this.name = name;
    }

    public InstanceJson(ModpackManifest modpack, ModpackVersionManifest versionManifest, boolean isPrivate, byte packType) {
        uuid = UUID.randomUUID();

        versionId = versionManifest.getId();
        id = modpack.getId();

        name = modpack.getName();

        version = versionManifest.getName();
        mcVersion = versionManifest.getTargetVersion("game");

        minMemory = versionManifest.getMinimumSpec();
        recMemory = versionManifest.getRecommendedSpec();
        memory = recMemory;
        adjustMemory();

    }

    public static InstanceJson load(Path path) throws IOException {
        return JsonUtils.parse(GSON, path, InstanceJson.class);
    }

    public static void save(Path path, InstanceJson properties) throws IOException {
        JsonUtils.write(GSON, path, properties);
    }

    private void adjustMemory() {
        HardwareAbstractionLayer hal = new SystemInfo().getHardware();
        long totalMemory = hal.getMemory().getTotal() / 1024 / 1024;
        if (recMemory > (totalMemory - 2048)) {
            memory = minMemory;
        }
    }
}
