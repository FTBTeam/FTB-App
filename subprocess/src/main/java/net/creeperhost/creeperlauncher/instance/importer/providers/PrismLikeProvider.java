package net.creeperhost.creeperlauncher.instance.importer.providers;

import com.google.gson.JsonElement;
import net.creeperhost.creeperlauncher.instance.importer.meta.InstanceSummary;
import net.creeperhost.creeperlauncher.util.GsonUtils;
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
    public InstanceSummary scanForInstance(Path instancePath) {
        Path metaPath = instancePath.resolve("mmc-pack.json");
        Path configPath = instancePath.resolve("instance.cfg");
        if (Files.notExists(metaPath) || Files.notExists(configPath)) {
            return null;
        }

        // Load the instance meta file
        var metaJson = GsonUtils.parseRawSafe(metaPath);
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

        return new InstanceSummary(name, instancePath, minecraftVersion, null);
    }

    @Override
    public Result<Boolean, String> importInstance(Path instancePath) {
        return Result.err("Not implemented");
    }
}
