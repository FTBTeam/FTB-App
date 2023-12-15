package net.creeperhost.creeperlauncher.instance.importer.providers;

import com.google.gson.JsonElement;
import net.covers1624.quack.platform.OperatingSystem;
import net.creeperhost.creeperlauncher.instance.importer.meta.SimpleInstanceInfo;
import net.creeperhost.creeperlauncher.util.GsonUtils;
import net.creeperhost.creeperlauncher.util.Result;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface InstanceProvider {
    Result<Boolean, String> importInstance(Path instancePath);

    /**
     * Retrieve all the instances that the provider holds
     * 
     * @param instancesPath User defined instance path. Can be null. When null, we detect the system and use the default path. 
     * @return list of instances within the providers instance path
     */
    List<SimpleInstanceInfo> instances(@Nullable Path instancesPath);

    @Nullable SimpleInstanceInfo instance(Path instancePath);
    
    default Path sourceLocation() {
        return sourceLocation(null);
    }
    
    default Path sourceLocation(@Nullable OperatingSystem os) {
        var operatingSystem = os;
        if (operatingSystem == null) {
            operatingSystem = OperatingSystem.current();
        }
        
        if (operatingSystem.isUnixLike() && !operatingSystem.isMacos()) {
            return linuxSourceLocation();
        }
        
        // Undecided on if defaulting to windows is a good idea
        return operatingSystem.isMacos() ? macosSourceLocation() : windowsSourceLocation();
    }
    
    Path windowsSourceLocation();
    
    Path macosSourceLocation();
    
    Path linuxSourceLocation();
    
    boolean isValidInstance(Path path);
    
    boolean isValidInstanceProvider(Path path);
    
    default boolean isCurseCompatible() {
        return false;
    }
    
    // Method to load a json file into a JsonObject
    @Nullable
    default JsonElement loadJson(Path path) {
        System.out.printf("Loading json from %s%n", path);
        try {
            JsonElement jsonElement = GsonUtils.loadJson(path, JsonElement.class);
            if (jsonElement == null || jsonElement.isJsonNull()) {
                return null;
            }
            
            return jsonElement;
        } catch (IOException e) {
            return null;
        }
    }
}
