package net.creeperhost.creeperlauncher.instance.importer.providers;

import net.creeperhost.creeperlauncher.instance.importer.meta.SimpleInstanceInfo;
import net.creeperhost.creeperlauncher.util.Result;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class GdLauncherProvider implements InstanceProvider {
    @Override
    public Result<Boolean, String> importInstance() {
        return null;
    }

    @Override
    public List<SimpleInstanceInfo> getAllInstances() {
        return null;
    }

    @Override
    public void getInstance(String instanceName) {

    }

    @Override
    public Path getDataLocation() {
        return null;
    }

    @Override
    public <T> Optional<T> getDataFile(Path path, Class<T> type) {
        return Optional.empty();
    }
}
