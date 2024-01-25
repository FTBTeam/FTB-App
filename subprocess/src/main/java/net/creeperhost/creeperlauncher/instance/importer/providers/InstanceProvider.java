package net.creeperhost.creeperlauncher.instance.importer.providers;

import net.creeperhost.creeperlauncher.instance.importer.meta.InstanceSummary;
import net.creeperhost.creeperlauncher.util.Result;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public interface InstanceProvider {

    /**
     * Called to scan a suspected instance folder.
     *
     * @param instancePath The path to scan.
     * @return The scanned instance result.
     */
    @Nullable InstanceSummary scanForInstance(Path instancePath);

    Result<Boolean, String> importInstance(Path instancePath);
}
