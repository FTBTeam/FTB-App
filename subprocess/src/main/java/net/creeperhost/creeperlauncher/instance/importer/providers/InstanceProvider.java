package net.creeperhost.creeperlauncher.instance.importer.providers;

import net.creeperhost.creeperlauncher.instance.importer.meta.SimpleInstanceInfo;
import net.creeperhost.creeperlauncher.instance.importer.transformers.MetaTransformer;
import net.creeperhost.creeperlauncher.util.Result;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface InstanceProvider {
    Result<Boolean, String> importInstance();
    
    List<SimpleInstanceInfo> getAllInstances();

    void getInstance(String instanceName);

    Path getDataLocation();
    
    <T> Optional<T> getDataFile(Path path, Class<T> type);
}
