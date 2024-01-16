package net.creeperhost.creeperlauncher.api.handlers.instances.backups;

import net.covers1624.quack.io.IOUtils;
import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.api.WebSocketHandler;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.pack.InstanceSnapshot;
import net.creeperhost.creeperlauncher.pack.Instance;
import net.creeperhost.creeperlauncher.util.FileUtils;
import net.creeperhost.creeperlauncher.util.ZipUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class InstanceRestoreBackupHandler implements IMessageHandler<InstanceRestoreBackupHandler.Request> {
    public static final Logger LOGGER = LogManager.getLogger();
    
    @Override
    public void handle(Request data) {
        Instance instance = Instances.getInstance(UUID.fromString(data.uuid));
        if (instance == null) {
            WebSocketHandler.sendMessage(new Reply(data, false, "Unable to locate modpack for backup restore"));
            return;
        }

        Path backupLocation = Path.of(data.backupLocation);
        if (Files.notExists(backupLocation)) {
            WebSocketHandler.sendMessage(new Reply(data, false, "Unable to locate backup file"));
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
        } catch (IOException e) {
            WebSocketHandler.sendMessage(new Reply(data, false, "Unable to read backup file"));
            return;
        }

        // Extract the zip in a tmp location
        InstanceSnapshot snapshotExecutor = instance.withSnapshot(localInstance -> {
                // Remove the overlapping files 
                basePaths.stream()
                    .map(e -> localInstance.getDir().resolve(e))
                    .forEach(e -> FileUtils.deletePath(e));
                
                // Extract from the backups location
                try {
                    ZipUtils.extractZip(backupLocation, localInstance.getDir());
                } catch (IOException e) {
                    LOGGER.error("Unable to extract zip files from {}", backupLocation, e);
                }
            })
            .specifyEffectedFiles(basePaths);
        
        try {
            snapshotExecutor.run();
            WebSocketHandler.sendMessage(new Reply(data, true, "Backup restored"));
        } catch (Throwable e) {
            WebSocketHandler.sendMessage(new Reply(data, false, "Unable to restore backup file"));
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
