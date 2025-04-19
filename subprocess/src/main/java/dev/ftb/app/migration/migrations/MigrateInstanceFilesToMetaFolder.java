package dev.ftb.app.migration.migrations;

import dev.ftb.app.Instances;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.other.RequestInstanceRefresh;
import dev.ftb.app.migration.Migration;
import dev.ftb.app.pack.Instance;
import dev.ftb.app.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;

public class MigrateInstanceFilesToMetaFolder implements Migration {
    private static final Logger LOGGER = LoggerFactory.getLogger(MigrateInstanceFilesToMetaFolder.class);
    
    @Override
    public String id() {
        return "migrate_instance_files_to_meta_folder";
    }

    @Override
    public boolean migrate() {
        var instances = Instances.allInstances();
        if (instances.isEmpty()) {
            return true; // Nothing to migrate
        }
        
        // Loop over all the instances and attempt to move the files to the meta folder
        for (var instance : instances) {
            var metaFolder = instance.metaPath;
            if (Files.notExists(metaFolder)) {
                try {
                    Files.createDirectories(metaFolder);
                } catch (Exception e) {
                    LOGGER.error("Failed to create meta folder for instance {}", instance.getUuid(), e);
                    continue;
                }
            }
            
            var targetFilesToMove = Map.of(
                instance.path.resolve(Instance.VERSION_FILE_NAME), instance.metaPath.resolve(Instance.VERSION_FILE_NAME),
                instance.path.resolve(".ftba/" + Instance.VERSION_MODS_FILE_NAME), instance.metaPath.resolve(Instance.VERSION_MODS_FILE_NAME),
                instance.path.resolve(Instance.MODIFICATIONS_FILE_NAME), instance.metaPath.resolve(Instance.MODIFICATIONS_FILE_NAME)
            );
            
            for (Map.Entry<Path, Path> sourceToTarget : targetFilesToMove.entrySet()) {
                // Ignore the file if it doesn't exist
                Path source = sourceToTarget.getKey();
                if (Files.notExists(source)) {
                    continue;
                }

                Path target = sourceToTarget.getValue();
                try {
                    Files.move(source, target);
                    LOGGER.info("Moved {} to {}", source, target);
                } catch (Exception e) {
                    LOGGER.error("Failed to move {} to {}", source, target, e);
                }
                
                // Remove the old file if it exists
                try {
                    if (Files.exists(source)) {
                        Files.delete(source);
                        LOGGER.info("Deleted {}", source);
                    }
                    
                    // If it's a directory, is it now empty?
                    if (Files.isDirectory(source) && FileUtils.listDir(source).isEmpty()) {
                        Files.delete(source);
                        LOGGER.info("Deleted empty directory {}", source);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to delete {}", source, e);
                }
            }
            
            // Now migrate the artwork files
            migrationArtwork(instance);
            
            // Finally, attempt to remove the folder.jpeg as windows hardly supports it anymore
            var folderJpeg = instance.path.resolve("folder.jpg");
            if (Files.exists(folderJpeg)) {
                try {
                    Files.delete(folderJpeg);
                    LOGGER.info("Deleted {}", folderJpeg);
                } catch (Exception e) {
                    LOGGER.error("Failed to delete {}", folderJpeg, e);
                }
            }
        }
        
        return true;
    }

    private void migrationArtwork(Instance instance) {
        if (instance.props.art == null || instance.props.art.isEmpty()) {
            return; // It's already migrated.
        }

        var artwork = instance.props.art;
        if (!artwork.startsWith("data:image/png;base64,")) {
            return; // Not a base64 image.
        }

        var decoded = Base64.getDecoder().decode(artwork.substring("data:image/png;base64,".length()));
        try (var is = new ByteArrayInputStream(decoded)) {
            instance.updateArtwork(is, "png");

            // Assume the artwork is now migrated.
            instance.props.art = null;
            instance.saveJson();
        } catch (IOException ex) {
            LOGGER.error("Failed to import art.", ex);
        }
    }
    
    @Override
    public void cleanup() {
        // Not super needed but might as well
        Instances.refreshInstances();
        WebSocketHandler.sendMessage(new RequestInstanceRefresh());
    }
}
