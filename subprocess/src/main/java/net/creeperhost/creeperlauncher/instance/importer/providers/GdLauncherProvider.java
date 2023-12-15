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
import java.util.List;
import java.util.Objects;

/**
 * TODO: GdLauncher is like 3 different launchers at this point so we might want to support their new systems as well once they're release.
 *       this code only handles their current mainline launcher.
 */
public class GdLauncherProvider implements InstanceProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(GdLauncherProvider.class);

    @Override
    public Result<Boolean, String> importInstance(Path instancePath) {
        return Result.err("Not implemented");
    }

    @Override
    public List<SimpleInstanceInfo> instances(Path instancesPath) {
        if (instancesPath == null) {
            instancesPath = sourceLocation();
        }
        
        if (!Files.exists(instancesPath)) {
            LOGGER.error("Failed to get instances dir");
            return List.of();
        }

        // Read the dir list
        return FileUtils.listDir(instancesPath)
            .stream()
            .map(this::instance)
            .filter(Objects::nonNull)
            .toList();
    }

    @Override
    @Nullable
    public SimpleInstanceInfo instance(Path instanceLocation) {
        if (!isValidInstance(instanceLocation)) {
            return null;
        }

        var instanceData = this.loadJson(instanceLocation.resolve("config.json"));
        if (instanceData == null) {
            return null;
        }

        // Handle errors (basically just throw)
        var name = GsonUtils.getNestedField("name", instanceData, JsonElement::getAsString);
        var minecraftVersion = GsonUtils.getNestedField("loader.mcVersion", instanceData, JsonElement::getAsString);
        
        // TODO: Where do we get java from
        return new SimpleInstanceInfo(name, instanceLocation, minecraftVersion, null);
    }

    @Override
    public Path windowsSourceLocation() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Path macosSourceLocation() {
        // TODO: Is next their main launcher?
        return Path.of("/Users/michael/Library/Application Support/gdlauncher_next/instances");
    }

    @Override
    public Path linuxSourceLocation() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean isValidInstance(Path path) {
        return Files.exists(path.resolve("config.json"));
    }

    @Override
    public boolean isValidInstanceProvider(Path path) {
        return false;
    }
}