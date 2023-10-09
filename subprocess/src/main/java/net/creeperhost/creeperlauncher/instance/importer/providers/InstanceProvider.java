package net.creeperhost.creeperlauncher.instance.importer.providers;

import com.google.gson.JsonElement;
import net.creeperhost.creeperlauncher.instance.importer.meta.SimpleInstanceInfo;
import net.creeperhost.creeperlauncher.instance.importer.transformers.MetaTransformer;
import net.creeperhost.creeperlauncher.util.Result;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface InstanceProvider {
    Result<Boolean, String> importInstance(String identifier);
    
    List<SimpleInstanceInfo> getAllInstances();

    @Nullable SimpleInstanceInfo getInstance(String instanceName);

    @Nullable Path getDataLocation();
    
    @Nullable JsonElement getDataFile(Path path);
}
