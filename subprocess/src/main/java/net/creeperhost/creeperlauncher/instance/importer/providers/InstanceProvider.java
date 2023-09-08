package net.creeperhost.creeperlauncher.instance.importer.providers;

import net.covers1624.quack.platform.OperatingSystem;
import net.creeperhost.creeperlauncher.util.Result;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public abstract class InstanceProvider {
    abstract Result<Boolean, String> importInstance();
    
    abstract List<String> getAllInstances();

    abstract void getInstance(String instanceName);

    abstract Path getDataLocation();
    
    abstract <T> Optional<T> getDataFile(Path path, Class<T> type);
}
