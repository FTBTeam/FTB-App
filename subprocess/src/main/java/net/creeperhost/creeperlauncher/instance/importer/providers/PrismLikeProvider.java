package net.creeperhost.creeperlauncher.instance.importer.providers;

import com.google.gson.JsonElement;
import net.creeperhost.creeperlauncher.instance.importer.meta.SimpleInstanceInfo;
import net.creeperhost.creeperlauncher.util.Result;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public abstract class PrismLikeProvider implements InstanceProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(PrismLikeProvider.class);
    
    @Override
    @Nullable
    public SimpleInstanceInfo instance(Path instancePath) {
        if (!isValidInstance(instancePath)) {
            return null;
        }
        
        // Load the instance meta file
        var metaFile = instancePath.resolve("mmc-pack.json");
        var metaJson = this.loadJson(metaFile);
        
        if (metaJson == null) {
            return null;
        }
        
        // Load the instance cfg file
        Properties instanceCfg = new Properties();
        try {
            instanceCfg.load(Files.newInputStream(instancePath.resolve("instance.cfg")));
        } catch (Exception e) {
            LOGGER.error("Failed to read instance.cfg", e);
            return null;
        }
        
        var name = instanceCfg.getProperty("name");
        
        // Game version
        var components = metaJson.getAsJsonObject().get("components");
        if (components.isJsonNull()) {
            LOGGER.error("Failed to read instance.json");
            return null;
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
            return null;
        }
        
        return new SimpleInstanceInfo(name, instancePath, minecraftVersion, null);
    }

    @Override
    public Result<Boolean, String> importInstance(Path instancePath) {
        return Result.err("Not implemented");
    }

    @Override
    public boolean isValidInstance(Path path) {
        return Files.exists(path.resolve("instance.cfg")) && Files.exists(path.resolve("mmc-pack.json"));
    }

    @Override
    public boolean isValidInstanceProvider(Path path) {
        return false;
    }
}