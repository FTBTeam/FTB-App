package net.creeperhost.creeperlauncher.api.handlers.instances.backups;

import com.google.common.reflect.TypeToken;
import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.instance.DestructiveInstanceAction;
import net.creeperhost.creeperlauncher.pack.LocalInstance;
import net.creeperhost.creeperlauncher.util.GsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class InstanceRestoreBackupHandler implements IMessageHandler<InstanceRestoreBackupHandler.InstanceRestoreRequestData> {
    public static final Logger LOGGER = LogManager.getLogger();
    
    @Override
    public void handle(InstanceRestoreRequestData data) {
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

        Set<String> basePaths = new HashSet<>();
        try (var zip = new ZipFile(backupLocation.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = entries.nextElement();
                if (zipEntry.isDirectory()) {
                    basePaths.add(zipEntry.getName().split("/\\//")[0]);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println(basePaths);

        // Extract the zip in a tmp location
        DestructiveInstanceAction.create(instance, (localInstance) -> {
            return DestructiveInstanceAction.Result.FATAL_ERROR;
        })
            .specifyEffectedFiles(Set.of("saves", "patchouli_books"))
            .run();
    }

    public static class InstanceRestoreRequestData extends BaseData {
        public String uuid;
        public String backupLocation;
    }
    
    private static class Reply extends InstanceRestoreRequestData {
        public String message;
        public boolean success;
        
        public Reply(InstanceRestoreRequestData data, boolean success, String message) {
            this.uuid = data.uuid;
            this.type = data.type + "Reply";
            this.message = message;
            this.success = success;
        }
    }
}
