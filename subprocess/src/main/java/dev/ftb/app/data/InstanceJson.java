package dev.ftb.app.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.annotations.JsonAdapter;
import dev.ftb.app.data.modpack.ModpackManifest;
import dev.ftb.app.data.modpack.ModpackVersionManifest;
import dev.ftb.app.storage.settings.Settings;
import dev.ftb.app.storage.settings.SettingsData;
import joptsimple.internal.Strings;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.gson.PathTypeAdapter;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

public class InstanceJson {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

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
    public int memory;

    public String jvmArgs = "";
    public String shellArgs;
    
    @Nullable
    public String programArgs; 
    
    public boolean embeddedJre = true;
    @Nullable
    @JsonAdapter (PathTypeAdapter.class)
    public Path jrePath = null;
    public int width;
    public int height;
    public boolean fullscreen = false;
    public String modLoader = "";

    public boolean isModified;
    public boolean isImport;
    public boolean hasInstMods;
    public boolean installComplete;
    
    @Deprecated
    public String category = "Default";
    
    @Nullable
    public UUID categoryId = null;
    
    public String releaseChannel = "unset";
    
    public boolean locked = true;
    public boolean pinned = false;

    /**
     * When enabled, the app will not inject mods from the meta system. Sometimes these can 
     * cause issues, so it's good to let the user have the option to turn them off
     */
    public boolean preventMetaModInjection;
    public boolean preventMetaAgentInjection;
    
    public byte packType;
    // TODO migrate this to `isPrivate`
    public boolean _private;

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
        programArgs = other.programArgs;
        embeddedJre = other.embeddedJre;
        jrePath = other.jrePath;
        width = other.width;
        height = other.height;
        modLoader = other.modLoader;
        isModified = other.isModified;
        isImport = other.isImport;
        hasInstMods = other.hasInstMods;
        installComplete = other.installComplete;
        fullscreen = other.fullscreen;
        packType = other.packType;
        _private = other._private;
        totalPlayTime = other.totalPlayTime;
        lastPlayed = other.lastPlayed;
        art = other.art;
        category = other.category;
        categoryId = other.categoryId;
        releaseChannel = other.releaseChannel;
        locked = other.locked;
        shellArgs = other.shellArgs;
        pinned = other.pinned;
        potentiallyBrokenDismissed = other.potentiallyBrokenDismissed;
        preventMetaModInjection = other.preventMetaModInjection;
        preventMetaAgentInjection = other.preventMetaAgentInjection;
    }

    // Copy instance.
    public InstanceJson(InstanceJson other, UUID uuid, String name) {
        this(other);
        this.uuid = uuid;
        this.name = name;
    }

    // New instance.
    public InstanceJson(Settings settings, ModpackManifest modpack, ModpackVersionManifest versionManifest, String mcVersion, boolean isPrivate, byte packType, String[] jvmArgs) {
        uuid = UUID.randomUUID();

        versionId = versionManifest.getId();
        id = modpack.getId();

        name = modpack.getName();

        version = versionManifest.getName();
        this.mcVersion = mcVersion;

        minMemory = versionManifest.getMinimumSpec();
        recMemory = versionManifest.getRecommendedSpec();
        
        memory = settings.get().instanceDefaults().memory();
        shellArgs = settings.get().instanceDefaults().shellArgs();
        programArgs = settings.get().instanceDefaults().getProgramArgs();
        width = settings.get().instanceDefaults().width();
        height = settings.get().instanceDefaults().height();
        preventMetaModInjection = settings.get().instanceDefaults().preventMetaModInjection();
        preventMetaAgentInjection = settings.get().instanceDefaults().preventMetaAgentInjection();
        
        this._private = isPrivate;
        this.packType = packType;
        this.jvmArgs = Strings.join(jvmArgs, " ");
    }

    public static InstanceJson load(Path path, SettingsData.InstanceSettings defaults) throws IOException {
        // Parse to generic obj
        JsonObject obj = JsonUtils.parse(GSON, path, JsonObject.class);

        // Parse to instance json.
        InstanceJson json = GSON.fromJson(obj, InstanceJson.class);
        json.applyMissingDefaults(obj, defaults);
        return json;
    }

    /// Older instance.json files can predate some fields, which Gson then leaves as null, 0 or false. Fill those from
    /// the user's instance defaults instead. The raw json is checked so a value that really is 0 or false is kept.
    private void applyMissingDefaults(JsonObject obj, SettingsData.InstanceSettings defaults) {
        if (!obj.has("memory")) memory = defaults.memory();
        if (!obj.has("programArgs")) programArgs = defaults.getProgramArgs();
        if (!obj.has("width")) width = defaults.width();
        if (!obj.has("height")) height = defaults.height();
        if (!obj.has("preventMetaModInjection")) preventMetaModInjection = defaults.preventMetaModInjection();
        if (!obj.has("preventMetaAgentInjection")) preventMetaAgentInjection = defaults.preventMetaAgentInjection();
        // Launching splits this, so it can never be null
        if (shellArgs == null) shellArgs = defaults.shellArgs();
    }

    public static InstanceJson load(byte[] bytes) throws IOException {
        return JsonUtils.parse(GSON, new ByteArrayInputStream(bytes), InstanceJson.class);
    }

    public static void save(Path path, InstanceJson properties ) throws IOException {
        JsonUtils.write(GSON, path, properties);
    }
}
