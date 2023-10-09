package net.creeperhost.creeperlauncher.instance.importer.providers;

import com.google.gson.JsonElement;
import net.creeperhost.creeperlauncher.instance.importer.meta.SimpleInstanceInfo;
import net.creeperhost.creeperlauncher.util.FileUtils;
import net.creeperhost.creeperlauncher.util.GsonUtils;
import net.creeperhost.creeperlauncher.util.Result;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

public abstract class PrismLikeProvider implements InstanceProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrismLikeProvider.class);
    
    @Override
    public Result<Boolean, String> importInstance(String identifier) {
        return Result.err("Not implemented");
    }

    @Override
    public List<SimpleInstanceInfo> getAllInstances() {
        var instancesPath = getDataLocation();
        if (instancesPath == null || Files.notExists(instancesPath)) {
            LOGGER.error("Failed to get instances dir {}", instancesPath);
            return List.of();
        }
        
        // Read the dir list
        var instanceLocations = FileUtils.listDir(instancesPath)
            .stream()
            .filter(e -> Files.exists(e.resolve("instance.cfg")) && Files.exists(e.resolve("mmc-pack.json")))
            .toList();
        
        List<SimpleInstanceInfo> simpleData = new LinkedList<>();
        for (Path location : instanceLocations) {
            try {
                Properties instanceCfg = new Properties();
                instanceCfg.load(Files.newInputStream(location.resolve("instance.cfg")));

                var instanceName = instanceCfg.getProperty("name");
                
                JsonElement instanceJson = GsonUtils.loadJson(location.resolve("mmc-pack.json"), JsonElement.class);
                var components = instanceJson.getAsJsonObject().get("components");
                if (components.isJsonNull()) {
                    LOGGER.error("Failed to read instance.json");
                    continue;
                }
                
                String minecraftVersion = null;
                var componentList = components.getAsJsonArray().asList();
                for (JsonElement component : componentList) {
                    var componentObj = component.getAsJsonObject();
                    if (componentObj.get("uid").getAsString().equals("net.minecraft")) {
                        minecraftVersion = componentObj.get("version").getAsString();
                        break;
                    }
                }
                
                if (minecraftVersion == null) {
                    LOGGER.error("Failed to read instance.json");
                    continue;
                }
                
                simpleData.add(new SimpleInstanceInfo(instanceName, location, minecraftVersion, null));
            } catch (Exception e) {
                LOGGER.error("Failed to read instance.json", e);
            }
        }
        
        return simpleData;
    }

    @Override
    @Nullable
    public SimpleInstanceInfo getInstance(String instanceName) {
        return null;
    }

    @Override
    @Nullable
    public JsonElement getDataFile(Path path) {
        return null;
    }
}