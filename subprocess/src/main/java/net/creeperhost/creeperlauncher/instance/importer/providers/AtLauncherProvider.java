package net.creeperhost.creeperlauncher.instance.importer.providers;

import com.google.gson.JsonElement;
import net.creeperhost.creeperlauncher.instance.importer.meta.SimpleInstanceInfo;
import net.creeperhost.creeperlauncher.util.GsonUtils;
import net.creeperhost.creeperlauncher.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public class AtLauncherProvider implements InstanceProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(AtLauncherProvider.class);

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
