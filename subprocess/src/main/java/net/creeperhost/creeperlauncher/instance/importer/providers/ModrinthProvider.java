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

public class ModrinthProvider implements InstanceProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModrinthProvider.class);
    
    @Override
    public List<SimpleInstanceInfo> instances(Path instancesPath) {
        if (instancesPath == null) {
            instancesPath = sourceLocation();
        }
        
        if (Files.notExists(instancesPath)) {
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
    public SimpleInstanceInfo instance(Path instancePath) {
        if (!isValidInstance(instancePath)) {
            return null;
        }
        
        var instanceData = this.loadJson(instancePath.resolve("profile.json"));
        if (instanceData == null) {
            return null;
        }
        
        // Handle errors (basically just throw)
        // TODO: java version
        var name = GsonUtils.getNestedField("metadata.name", instanceData, JsonElement::getAsString);
        var minecraftVersion = GsonUtils.getNestedField("metadata.game_version", instanceData, JsonElement::getAsString);
        
        return new SimpleInstanceInfo(name, instancePath, minecraftVersion, null);
    }

    @Override
    public Result<Boolean, String> importInstance(Path instancePath) {
        return Result.err("Not implemented");
    }

    @Override
    public Path windowsSourceLocation() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Path macosSourceLocation() {
        return Path.of(System.getProperty("user.home"), "/Library/Application Support/com.modrinth.theseus/profiles/");
    }

    @Override
    public Path linuxSourceLocation() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean isValidInstance(Path path) {
        return Files.exists(path.resolve("profile.json"));
    }

    @Override
    public boolean isValidInstanceProvider(Path path) {
        return false;
    }
}
