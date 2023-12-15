package net.creeperhost.creeperlauncher.instance.importer.providers;

import java.nio.file.Path;

public class MultiMcProvider extends PrismLikeProvider {
    
    @Override
    public Path windowsSourceLocation() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Path macosSourceLocation() {
        return Path.of("/Applications/MultiMC.app/Data/instances/")
    }

    @Override
    public Path linuxSourceLocation() {
        throw new RuntimeException("Not implemented");
    }
}
