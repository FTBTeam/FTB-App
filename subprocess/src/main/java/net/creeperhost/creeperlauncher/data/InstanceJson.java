package net.creeperhost.creeperlauncher.data;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.JsonAdapter;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.gson.PathTypeAdapter;
import net.creeperhost.creeperlauncher.data.modpack.ModpackManifest;
import net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest;
import net.creeperhost.creeperlauncher.storage.settings.Settings;
import org.jetbrains.annotations.Nullable;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;

import java.io.ByteArrayInputStream;
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
    public int memory = Settings.getSettings().instanceDefaults().memory();

    public String jvmArgs = Settings.getSettings().instanceDefaults().javaArgs();
    public String shellArgs = Settings.getSettings().instanceDefaults().shellArgs();
    public boolean embeddedJre = Boolean.parseBoolean(Settings.settings.getOrDefault("embeddedJre", "true"));
    public String envArgs = Settings.settings.getOrDefault("envArgs", "");
    @Nullable
    @JsonAdapter (PathTypeAdapter.class)
    public Path jrePath = null;
    public int width = Settings.getSettings().instanceDefaults().width();
    public int height = Settings.getSettings().instanceDefaults().height();
    public boolean fullscreen = false;
    public String modLoader = "";

    public boolean isModified;
    public boolean isImport;
    public boolean cloudSaves;
    public boolean hasInstMods;
    public boolean installComplete;
    public String category = "Default";
    public String releaseChannel = "unset";
    
    public boolean locked = true;
    public boolean pinned = false;
    
    public byte packType;
    // TODO migrate this to `isPrivate`
    public boolean _private;
    
    // When locked the instance can not have mods changed
    public boolean isLocked = true;

    /**
     * The current play time in millis.
     */
    public long totalPlayTime;
    public long lastPlayed;

    @Nullable
    @Deprecated // TODO Replace with Artv2 system.
    public String art;
    
    public boolean potentiallyBrokenDismissed = false;

    // Gson
    InstanceJson() {
    }

    public InstanceJson(InstanceJson other) {
        uuid = other.uuid;
        id = other.id;
        versionId = other.versionId;
        name = other.name;
        version = other.version;
        mcVersion = other.mcVersion;
        minMemory = other.minMemory;
        recMemory = other.recMemory;
        memory = other.memory;
        jvmArgs = other.jvmArgs;
        embeddedJre = other.embeddedJre;
        jrePath = other.jrePath;
        width = other.width;
        height = other.height;
        modLoader = other.modLoader;
        isModified = other.isModified;
        isImport = other.isImport;
        cloudSaves = other.cloudSaves;
        hasInstMods = other.hasInstMods;
        installComplete = other.installComplete;
        fullscreen = other.fullscreen;
        packType = other.packType;
        _private = other._private;
        totalPlayTime = other.totalPlayTime;
        lastPlayed = other.lastPlayed;
        art = other.art;
        category = other.category;
        releaseChannel = other.releaseChannel;
        locked = other.locked;
        shellArgs = other.shellArgs;
        pinned = other.pinned;
        potentiallyBrokenDismissed = other.potentiallyBrokenDismissed;
    }
    
    public static InstanceJson create(
            long id, long versionId, String name, String version, String mcVersion, int minMemory, int recMemory, 
            int memory, String jvmArgs, boolean embeddedJre, Path jrePath, int width, int height, String modLoader,
            boolean isModified, boolean isImport, boolean cloudSaves, boolean hasInstMods, boolean installComplete,
            byte packType, boolean _private, long totalPlayTime, long lastPlayed, String art,
            String shellArgs, String envArgs, boolean isLocked
    ) {
        InstanceJson instanceJson = new InstanceJson();
        instanceJson.uuid = UUID.randomUUID();
        instanceJson.id = id;
        instanceJson.versionId = versionId;
        instanceJson.name = name;
        instanceJson.version = version;
        instanceJson.mcVersion = mcVersion;
        instanceJson.minMemory = minMemory;
        instanceJson.recMemory = recMemory;
        instanceJson.memory = memory;
        instanceJson.jvmArgs = jvmArgs;
        instanceJson.embeddedJre = embeddedJre;
        instanceJson.jrePath = jrePath;
        instanceJson.width = width;
        instanceJson.height = height;
        instanceJson.modLoader = modLoader;
        instanceJson.isModified = isModified;
        instanceJson.isImport = isImport;
        instanceJson.cloudSaves = cloudSaves;
        instanceJson.hasInstMods = hasInstMods;
        instanceJson.installComplete = installComplete;
        instanceJson.packType = packType;
        instanceJson._private = _private;
        instanceJson.totalPlayTime = totalPlayTime;
        instanceJson.lastPlayed = lastPlayed;
        instanceJson.art = art;
        instanceJson.shellArgs = shellArgs;
        instanceJson.envArgs = envArgs;
        instanceJson.isLocked = isLocked;
        return instanceJson;
    }

    // Copy instance.
    public InstanceJson(InstanceJson other, UUID uuid, String name) {
        this(other);
        this.uuid = uuid;
        this.name = name;
    }

    // New instance.
    public InstanceJson(ModpackManifest modpack, ModpackVersionManifest versionManifest, String mcVersion, boolean isPrivate, byte packType) {
        uuid = UUID.randomUUID();

        versionId = versionManifest.getId();
        id = modpack.getId();

        name = modpack.getName();

        version = versionManifest.getName();
        this.mcVersion = mcVersion;

        minMemory = versionManifest.getMinimumSpec();
        recMemory = versionManifest.getRecommendedSpec();
        memory = recMemory;
        adjustMemory();

        this._private = isPrivate;
        this.packType = packType;
    }

    public static InstanceJson load(Path path) throws IOException {
        // Parse to generic obj
        JsonObject obj = JsonUtils.parse(GSON, path, JsonObject.class);
                
        // Parse to instance json.
        return GSON.fromJson(obj, InstanceJson.class);
    }

    public static InstanceJson load(byte[] bytes) throws IOException {
        return JsonUtils.parse(GSON, new ByteArrayInputStream(bytes), InstanceJson.class);
    }

    public static void save(Path path, InstanceJson properties ) throws IOException {
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
