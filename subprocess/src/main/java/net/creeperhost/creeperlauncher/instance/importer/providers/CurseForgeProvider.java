package net.creeperhost.creeperlauncher.instance.importer.providers;

import com.google.gson.JsonElement;
import net.creeperhost.creeperlauncher.instance.importer.meta.SimpleInstanceInfo;
import net.creeperhost.creeperlauncher.util.GsonUtils;
import net.creeperhost.creeperlauncher.util.Result;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @implNote
 *  - Settings file always contains the modded folder
 *  - Instances folder is always called instances
 *  - Curseforge instances do not set their own java version so we'll need to figure that bit out
 */
public class CurseForgeProvider implements InstanceProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(CurseForgeProvider.class);

    @Override
    public @Nullable Path extendedSourceLocation() {
        return getModdedDir();
    }

    @Override
    @Nullable
    public SimpleInstanceInfo instance(Path instancePath) {
        if (!isValidInstance(instancePath)) {
            return null;
        }
        
        // Load the instance meta file
        var metaFile = instancePath.resolve("minecraftinstance.json");
        var metaJson = this.loadJson(metaFile);
        
        if (metaJson == null) {
            return null;
        }
        
        // TODO: Catch errors, any error for missing data will just be considered invalid
        // We need the modloader
        var name = GsonUtils.getNestedField("name", metaJson, JsonElement::getAsString);
        var gameVersion = GsonUtils.getNestedField("gameVersion", metaJson, JsonElement::getAsString);
        var installDate = GsonUtils.getNestedField("installDate", metaJson, JsonElement::getAsString);
        var modloader = GsonUtils.getNestedField("baseModLoader.name", metaJson, JsonElement::getAsString); // LOADER-VERSION
        
        // Enrich the data
        var lastPlayed = GsonUtils.getNestedField("lastPlayed", metaJson, JsonElement::getAsLong);
        
        // These will be 0 if they don't exist
        var curseProject = GsonUtils.getNestedField("projectID", metaJson, JsonElement::getAsInt);
        var curseFile = GsonUtils.getNestedField("fileID", metaJson, JsonElement::getAsInt);
        
        // Pull some nice to have info
        var memory = GsonUtils.getNestedField("allocatedMemory", metaJson, JsonElement::getAsInt);
        
        return new SimpleInstanceInfo(name, instancePath, gameVersion, installDate);
    }

    @Override
    public Result<Boolean, String> importInstance(Path identifier) {
        // Load the instance again
        var instance = instance(identifier);
        if (instance == null) {
            return Result.err("Failed to load instance");
        }
        
        // Now import it I guess
        // At the core of it all, we just need to copy a directory from one place to another

        return Result.err("Not implemented");
    }
    
    @Nullable
    public Path getModdedDir() {
        JsonElement settingJson = this.loadJson(this.sourceLocation().resolve("settings.json"));
        if (settingJson == null) {
            LOGGER.error("Failed to load settings file");
            return null;
        }
        
        String moddedDir = GsonUtils.getNestedField("minecraft.moddingFolder", settingJson, JsonElement::getAsString);
        
        if (moddedDir == null || moddedDir.isEmpty()) {
            LOGGER.error("Failed to load settings file");
            return null;
        }

        Path instancesPath = Path.of(moddedDir).resolve("Instances");
        if (Files.notExists(instancesPath)) {
            LOGGER.error("Instances folder does not exist");
            return null;
        }

        return instancesPath;
    }

    @Override
    public Path windowsSourceLocation() {
        return Path.of(System.getenv("APPDATA"), "CurseForge");
    }

    @Override
    public Path macosSourceLocation() {
        return Path.of(System.getProperty("user.home"), "Library", "Application Support", "CurseForge");
    }

    @Override
    public Path linuxSourceLocation() {
        return Path.of(System.getProperty("user.home"), ".config", "CurseForge");
    }
    
    @Override
    public boolean isValidInstance(Path path) {
        var metaFile = path.resolve("minecraftinstance.json");
        return Files.exists(metaFile);
    }

    @Override
    public boolean isValidInstanceProvider(Path path) {
        return false;
    }

    @Override
    public boolean isCurseCompatible() {
        return true;
    }
}
