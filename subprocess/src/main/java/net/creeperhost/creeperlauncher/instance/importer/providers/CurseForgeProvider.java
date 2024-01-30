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
 * @implNote - Settings file always contains the modded folder
 * - Instances folder is always called instances
 * - Curseforge instances do not set their own java version so we'll need to figure that bit out
 */
public class CurseForgeProvider extends InstanceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CurseForgeProvider.class);

    @Override
    @Nullable
    public InstanceSummary scanForInstance(Path instancePath) {

        // Load the instance meta file
        var metaFile = instancePath.resolve("minecraftinstance.json");
        if (Files.notExists(metaFile)) return null;

        var metaJson = GsonUtils.parseRawSafe(metaFile);

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

        return new InstanceSummary(name, instancePath, gameVersion, null, installDate);
    }

    @Override
    public InstalledInstancesHandler.SugaredInstanceJson importInstance(Path identifier) {
        throw new NotImplementedException("TODO");
    }
}
