package net.creeperhost.creeperlauncher.instance.importer.providers;

import com.google.gson.JsonElement;
import net.creeperhost.creeperlauncher.instance.importer.meta.InstanceSummary;
import net.creeperhost.creeperlauncher.util.GsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public class AtLauncherProvider extends InstanceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AtLauncherProvider.class);

    @Override
    public InstanceSummary scanForInstance(Path instancePath) {
        Path instanceJsonPath = instancePath.resolve("instance.json");
        if (Files.notExists(instanceJsonPath)) return null;

        var instanceData = GsonUtils.parseRawSafe(instancePath.resolve("instance.json"));
        if (instanceData == null) {
            return null;
        }

        // Handle errors (basically just throw
        var name = GsonUtils.getNestedField("launcher.name", instanceData, JsonElement::getAsString);
        var javaVersion = GsonUtils.getNestedField("javaVersion.majorVersion", instanceData, JsonElement::getAsInt);
        var mcVersion = GsonUtils.getNestedField("id", instanceData, JsonElement::getAsString); // This might be wrong

        return new InstanceSummary(name, instancePath, mcVersion, null, String.valueOf(javaVersion));
    }
}
