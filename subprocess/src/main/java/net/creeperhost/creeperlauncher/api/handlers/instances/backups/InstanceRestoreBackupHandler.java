package net.creeperhost.creeperlauncher.api.handlers.instances.backups;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
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
        Set<String> basePaths = new HashSet<>();
        try (var zipFile = new ZipFile(backupLocation.toFile())) {
            var entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                if (zipEntry.getName().equals("")) {
                    continue;
                }
                
                basePaths.add(zipEntry.getName().split("\\/")[0]);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        // Extract the zip in a tmp location
        instance.createSafeAction(localInstance -> {
                // Remove the overlapping files 
                basePaths.stream()
                    .map(e -> localInstance.getDir().resolve(e))
                    .forEach(e -> FileUtils.deletePath(e));
            
                try (var zipFile = new ZipFile(backupLocation.toFile())) {
                    var entries = zipFile.entries();
                    while (entries.hasMoreElements()) {
                        var zipEntry = entries.nextElement();
                        
                        // Seems that FTB Backups 2 doesn't actually create a directory, so we'll need to figure it out ourselves
                        var file = Path.of(zipEntry.getName());
                        var instanceOutputLocation = localInstance.getDir().resolve(file);
                        
                        if (Files.notExists(instanceOutputLocation.getParent())) {
                            Files.createDirectories(instanceOutputLocation.getParent());
                        }
                        
                        if (!zipEntry.isDirectory()) {
                            try (
                                var inputStream = zipFile.getInputStream(zipEntry);
                                var output = Files.newOutputStream(instanceOutputLocation);
                            ) {
                                output.write(inputStream.readAllBytes());
                            }
                        }
                    }
                } catch (IOException e) {
                    LOGGER.error("Unable to use provided zip, {}", backupLocation, e);
                    return DestructiveInstanceAction.Result.FATAL_ERROR;
                }

                return DestructiveInstanceAction.Result.SUCCESS;
            })
            .specifyEffectedFiles(basePaths)
            .run();
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
            this.message = message;
            this.success = success;
        }
    }
}
