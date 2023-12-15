package net.creeperhost.creeperlauncher.instance.importer.providers;

import com.google.gson.JsonElement;
import net.creeperhost.creeperlauncher.instance.importer.meta.SimpleInstanceInfo;
import net.creeperhost.creeperlauncher.util.FileUtils;
import net.creeperhost.creeperlauncher.util.GsonUtils;
import net.creeperhost.creeperlauncher.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class AtLauncherProvider implements InstanceProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(AtLauncherProvider.class);

    /**
     * TODO I'm thinking this can be abstracted into two parts. A get source, and a loop over the provided dir for all the instances
     */
    @Override
    public List<SimpleInstanceInfo> instances(Path instancesLocation) {
        if (instancesLocation == null) {
            // No magic here like curseforge, instances are always here (as far as I know)
            instancesLocation = sourceLocation();
        }
        
        if (!Files.exists(instancesLocation)) {
            LOGGER.error("Failed to get instances dir");
            return List.of();
        }

        // Read the dir list 
        return FileUtils.listDir(instancesLocation)
            .stream()
            .map(this::instance)
            .filter(Objects::nonNull)
            .toList();
    }

    @Override
    public SimpleInstanceInfo instance(Path instanceLocation) {
        if (!isValidInstance(instanceLocation)) {
            return null;
        }
        
        var instanceData = this.loadJson(instanceLocation.resolve("instance.json"));
        if (instanceData == null) {
            return null;
        }
        
        // Handle errors (basically just throw
        var name = GsonUtils.getNestedField("launcher.name", instanceData, JsonElement::getAsString);
        var javaVersion = GsonUtils.getNestedField("javaVersion.majorVersion", instanceData, JsonElement::getAsInt);
        var mcVersion = GsonUtils.getNestedField("id", instanceData, JsonElement::getAsString); // This might be wrong
        
        return new SimpleInstanceInfo(name, instanceLocation, mcVersion, String.valueOf(javaVersion));
    }

    @Override
    public Result<Boolean, String> importInstance(Path instanceLocation) {
        return Result.err("Not implemented");
    }

    @Override
    public Path windowsSourceLocation() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Path macosSourceLocation() {
        return Path.of("/Applications/ATLauncher.app/Contents/Java/instances");
    }

    @Override
    public Path linuxSourceLocation() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean isValidInstance(Path path) {
        return Files.exists(path.resolve("instance.json"));
    }

    @Override
    public boolean isValidInstanceProvider(Path path) {
        return false;
    }
}
