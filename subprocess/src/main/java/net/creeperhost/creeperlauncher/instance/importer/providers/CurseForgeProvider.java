package net.creeperhost.creeperlauncher.instance.importer.providers;

import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.platform.OperatingSystem;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.data.InstanceJson;
import net.creeperhost.creeperlauncher.instance.importer.meta.CurseForgeSettings;
import net.creeperhost.creeperlauncher.pack.Instance;
import net.creeperhost.creeperlauncher.util.GsonUtils;
import net.creeperhost.creeperlauncher.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * @implNote
 *  - Settings file always contains the modded folder
 *  - Instances folder is always called instances
 */
public class CurseForgeProvider extends InstanceProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(CurseForgeProvider.class);

    public static void main(String[] args) {
        CurseForgeProvider provider = new CurseForgeProvider();
        provider.getAllInstances().forEach(System.out::println);
    }
    
    @Override
    List<String> getAllInstances() {
        Optional<CurseForgeSettings> settings = getSettings();
        if (settings.isEmpty()) {
            LOGGER.error("Failed to get settings");
            return List.of();
        }

        CurseForgeSettings.Minecraft minecraft = settings.get().minecraft();
        if (minecraft == null) {
            LOGGER.error("Failed to get minecraft settings");
            return List.of();
        }
        
        Path path = minecraft.moddingFolder();
        if (path == null) {
            LOGGER.error("Failed to get modding folder");
            return List.of();
        }
        
        Path instancesPath = path.resolve("Instances");
        if (Files.notExists(instancesPath)) {
            LOGGER.error("Instances folder does not exist");
            return List.of();
        }
        
        try {
            return Files.list(instancesPath)
                    .filter(Files::isDirectory)
                    .map(Path::toString)
                    .toList();
        } catch (Exception e) {
            LOGGER.error("Failed to list instances", e);
        }
        
        return List.of();
    }

    @Override
    void getInstance(String instanceName) {

    }

    @Override
    Path getDataLocation() {
        OperatingSystem os = OperatingSystem.current();
        if (os.isMacos()) {
            // Example: /Users/michael/Library/Application Support/CurseForge/settings.json
            return Path.of(System.getProperty("user.home"), "Library", "Application Support", "CurseForge");
        }
        
        return Path.of("");
    }
    
    @Override
    <T> Optional<T> getDataFile(Path path, Class<T> type) {
        Path dataLocation = getDataLocation();
        Path instancePath = dataLocation.resolve(path);
        Path metaFile = instancePath.resolve("minecraftinstance.json");
        try {
            return Optional.of(GsonUtils.loadJson(metaFile, type));
        } catch (Exception e) {
            LOGGER.error("Failed to load instance meta file", e);
        }
        
        return Optional.empty();
    }
    
    private Optional<CurseForgeSettings> getSettings() {
        Path dataLocation = getDataLocation();
        Path settingsFile = dataLocation.resolve("settings.json");
        if (Files.notExists(settingsFile)) {
            return Optional.empty();
        }
        
        try {
            return Optional.of(GsonUtils.loadJson(settingsFile, CurseForgeSettings.class));
        } catch (Exception e) {
            LOGGER.error("Failed to load settings file", e);
        }
        
        return Optional.empty();
    }

    @Override
    Result<Boolean, String> importInstance() {
        List<String> allInstances = getAllInstances();
        for (String instance : allInstances) {
            var ftbInstance = InstanceJson.create(
                
            );
        }
        
        return Result.err("Not implemented");
    }
}
