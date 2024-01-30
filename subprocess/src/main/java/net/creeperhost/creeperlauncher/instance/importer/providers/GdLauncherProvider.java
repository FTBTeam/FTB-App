package net.creeperhost.creeperlauncher.instance.importer.providers;

import com.google.gson.JsonElement;
import net.creeperhost.creeperlauncher.api.handlers.instances.InstalledInstancesHandler;
import net.creeperhost.creeperlauncher.instance.importer.meta.InstanceSummary;
import net.creeperhost.creeperlauncher.util.GsonUtils;
import net.creeperhost.creeperlauncher.util.Result;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * TODO: GdLauncher is like 3 different launchers at this point so we might want to support their new systems as well once they're release.
 *       this code only handles their current mainline launcher.
 */
public class GdLauncherProvider extends InstanceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(GdLauncherProvider.class);

    @Override
    @Nullable
    public InstanceSummary scanForInstance(Path instancePath) {
        Path configPath = instancePath.resolve("config.json");
        if (Files.notExists(configPath)) return null;

        var instanceData = GsonUtils.parseRawSafe(configPath);
        if (instanceData == null) {
            return null;
        }

        // Handle errors (basically just throw)
        var name = GsonUtils.getNestedField("name", instanceData, JsonElement::getAsString);
        var minecraftVersion = GsonUtils.getNestedField("loader.mcVersion", instanceData, JsonElement::getAsString);

        // TODO: Where do we get java from
        return new InstanceSummary(name, instancePath, minecraftVersion, null, null);
    }

    @Override
    public InstalledInstancesHandler.SugaredInstanceJson importInstance(Path instancePath) {
        throw new NotImplementedException("TODO");
    }
}
