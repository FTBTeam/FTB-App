package net.creeperhost.creeperlauncher.instance.importer.providers;

import java.nio.file.Path;

public class PrismProvider extends PrismLikeProvider {
    @Override
    public Path windowsSourceLocation() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Path macosSourceLocation() {
        return Path.of(System.getProperty("user.home"), "/Library/Application Support/PrismLauncher/instances/");
    }

    @Override
    public Path linuxSourceLocation() {
        throw new RuntimeException("Not implemented");
    }
}
