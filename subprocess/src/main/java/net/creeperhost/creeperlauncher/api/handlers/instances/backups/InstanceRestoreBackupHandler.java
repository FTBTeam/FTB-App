package net.creeperhost.creeperlauncher.api.handlers.instances.backups;

import net.covers1624.quack.io.IOUtils;
import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.instance.DestructiveInstanceAction;
import net.creeperhost.creeperlauncher.pack.LocalInstance;
import net.creeperhost.creeperlauncher.util.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class InstanceRestoreBackupHandler implements IMessageHandler<InstanceRestoreBackupHandler.Request> {
    public static final Logger LOGGER = LogManager.getLogger();
    
    @Override
    public void handle(Request data) {
        LocalInstance instance = Instances.getInstance(UUID.fromString(data.uuid));
        if (instance == null) {
            Settings.webSocketAPI.sendMessage(new Reply(data, false, "Unable to locate modpack for backup restore"));
            return;
        }

        Path backupLocation = Path.of(data.backupLocation);
        if (Files.notExists(backupLocation)) {
            Settings.webSocketAPI.sendMessage(new Reply(data, false, "Unable to locate backup file"));
            return;
        }

        // Find the files in the zip to know what we need to recover if any of this goes wrong
        Set<String> basePaths;
        try (var zipFs = IOUtils.getJarFileSystem(backupLocation, true)) {
            Path zipRoot = zipFs.getPath("");
            try (var zipRootFiles = Files.walk(zipRoot, 1)) {
                basePaths = zipRootFiles
                    .filter(e -> !e.toString().isEmpty())
                    .map(e -> e.getFileName().toString().split("\\/")[0])
                    .collect(Collectors.toSet());
            }

            // Extract the zip in a tmp location
            var result = instance.createSafeAction(localInstance -> {
                    // Remove the overlapping files 
                    basePaths.stream()
                        .map(e -> localInstance.getDir().resolve(e))
                        .forEach(e -> FileUtils.deletePath(e));

                    // Seems that FTB Backups 2 doesn't actually create a directory, so we'll need to figure it out ourselves
                    try (var files = Files.walk(zipRoot)) {
                        for (Path path : files.filter(e -> !e.toString().isEmpty()).toList()) {
                            var instanceOutputLocation = localInstance.getDir().resolve(path.toString());

                            if (Files.notExists(instanceOutputLocation.getParent())) {
                                Files.createDirectories(instanceOutputLocation.getParent());
                            }

                            if (!Files.isDirectory(path)) {
                                Files.copy(path, instanceOutputLocation);
                            }   
                        }
                    } catch (IOException e) {
                        LOGGER.error("Unable to read zip files from {} - {}", zipRoot, backupLocation, e);
                        return DestructiveInstanceAction.Result.FATAL_ERROR;
                    }

                    return DestructiveInstanceAction.Result.SUCCESS;
                })
                .specifyEffectedFiles(basePaths)
                .run();

            if (result.isFailure()) {
                Settings.webSocketAPI.sendMessage(new Reply(data, false, "Unable to restore backup file"));
                return;
            }

            Settings.webSocketAPI.sendMessage(new Reply(data, true, "Backup restored"));
        } catch (IOException e) {
            Settings.webSocketAPI.sendMessage(new Reply(data, false, "Unable to read backup file"));
        }
    }

    public static class Request extends BaseData {
        public String uuid;
        public String backupLocation;
    }
    
    private static class Reply extends Request {
        public String message;
        public boolean success;
        
        public Reply(Request data, boolean success, String message) {
            this.uuid = data.uuid;
            this.type = data.type + "Reply";
            this.requestId = data.requestId;
            this.message = message;
            this.success = success;
        }
    }
}
