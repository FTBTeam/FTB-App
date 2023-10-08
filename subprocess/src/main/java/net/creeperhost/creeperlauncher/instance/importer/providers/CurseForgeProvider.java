package net.creeperhost.creeperlauncher.instance.importer.providers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.covers1624.quack.platform.OperatingSystem;
import net.creeperhost.creeperlauncher.instance.importer.meta.SimpleInstanceInfo;
import net.creeperhost.creeperlauncher.util.GsonUtils;
import net.creeperhost.creeperlauncher.util.Result;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * @implNote
 *  - Settings file always contains the modded folder
 *  - Instances folder is always called instances
 */
public class CurseForgeProvider implements InstanceProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(CurseForgeProvider.class);
    
    @Override
    public List<SimpleInstanceInfo> getAllInstances() {
        var instancesPath = getModdedDir();
        if (instancesPath.isEmpty()) {
            LOGGER.error("Failed to get modded dir");
            return List.of();
        }
        
        try (var files = Files.list(instancesPath.get())) {
            var modpacks = files
                .filter(Files::isDirectory)
                .filter(path -> Files.exists(path.resolve("minecraftinstance.json")))
                .map(e -> Pair.of(e, this.getDataFile(e)))
                .filter(e -> e.getRight() != null)
                .map(e -> Pair.of(e.getLeft(), e.getRight().getAsJsonObject()))
                .toList();

            return modpacks
                .stream().map(pack -> new SimpleInstanceInfo(
                    pack.getValue().get("name").getAsString(),
                    pack.getKey(),
                    pack.getValue().get("gameVersion").getAsString(),
                    pack.getValue().get("installDate").getAsString() // No java data provided...
                )).toList();
        } catch (Exception e) {
            LOGGER.error("Failed to list instances", e);
        }
        
        return List.of();
    }

    @Override
    @Nullable
    public SimpleInstanceInfo getInstance(String instanceName) {
        return null;
    }
    
    public Optional<Path> getModdedDir() {
        var minecraftPath = this.loadJson(getDataLocation().resolve("settings.json"))
            .flatMap(e -> GsonUtils.getProperty("minecraft.moddingFolder", e, JsonElement::getAsString))
            .map(Path::of);

        if (minecraftPath.isEmpty()) {
            LOGGER.error("Failed to load settings file");
            return Optional.empty();
        }

        Path instancesPath = minecraftPath.get().resolve("Instances");
        if (Files.notExists(instancesPath)) {
            LOGGER.error("Instances folder does not exist");
            return Optional.empty();
        }
        
        return Optional.of(instancesPath);
    }

    @Override
    public Path getDataLocation() {
        return switch (OperatingSystem.current()) {
            case WINDOWS -> Path.of(System.getenv("APPDATA"), "CurseForge");
            case LINUX, SOLARIS, FREEBSD -> Path.of(System.getProperty("user.home"), ".config", "CurseForge");
            case MACOS -> Path.of(System.getProperty("user.home"), "Library", "Application Support", "CurseForge");
            default -> Path.of("");
        };
    }
    
    @Override
    @Nullable
    public JsonElement getDataFile(Path path) {
        Path dataLocation = getDataLocation();
        Path instancePath = dataLocation.resolve(path);
        Path metaFile = instancePath.resolve("minecraftinstance.json");
        try {
            return GsonUtils.loadJson(metaFile, JsonElement.class);
        } catch (Exception e) {
            LOGGER.error("Failed to load instance meta file", e);
        }
        
        return null;
    }
    
    private Optional<JsonElement> loadJson(Path path) {
        try {
            return Optional.of(GsonUtils.loadJson(path, JsonElement.class));
        } catch (Exception e) {
            LOGGER.error("Failed to load json file", e);
        }
        
        return Optional.empty();
    }

    @Override
    public Result<Boolean, String> importInstance(String identifier) {
//        List<String> allInstances = getAllInstances();
//        for (String instance : allInstances) {
//            var
                
//        }
        
        return Result.err("Not implemented");
    }
}
