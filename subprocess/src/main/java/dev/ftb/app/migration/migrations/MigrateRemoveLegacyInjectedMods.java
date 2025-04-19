package dev.ftb.app.migration.migrations;

import dev.ftb.app.Instances;
import dev.ftb.app.migration.Migration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.util.List;

public class MigrateRemoveLegacyInjectedMods implements Migration {
    private final Logger logger = LoggerFactory.getLogger(MigrateRemoveLegacyInjectedMods.class);
    
    @Override
    public String id() {
        return "migrate_remove_legacy_injected_mods";
    }

    @Override
    public boolean migrate() {
        var instances = Instances.allInstances();
        if (instances.isEmpty()) {
            return true; // Nothing to migrate
        }
        
        for (var instance : instances) {
            var filesToRemove = List.of(
                instance.path.resolve("Log4jPatcher-1.0.1.jar"),
                instance.path.resolve("mods/launchertray-1.0.jar"),
                instance.path.resolve("mods/launchertray-progress-1.0.jar"),
                instance.path.resolve("mods/ftb-hide.jar"),
                instance.path.resolve("mods/ftb-progress.jar")
            );
            
            for (var file : filesToRemove) {
                try {
                    if (Files.exists(file)) {
                        Files.delete(file);
                    }
                } catch (Exception e) {
                    // Ignore errors
                    logger.error("Failed to delete file {} for instance {}", file, instance.getUuid(), e);
                }
            }
        }
        
        return true;
    }
}
