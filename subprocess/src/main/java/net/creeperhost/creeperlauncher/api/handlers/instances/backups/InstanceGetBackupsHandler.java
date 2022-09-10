package net.creeperhost.creeperlauncher.api.handlers.instances.backups;

import com.google.common.reflect.TypeToken;
import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.pack.LocalInstance;
import net.creeperhost.creeperlauncher.util.GsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

public class InstanceGetBackupsHandler implements IMessageHandler<InstanceGetBackupsHandler.BackupsData> {
    public static final Logger LOGGER = LogManager.getLogger();
    
    @Override
    public void handle(BackupsData data) {
        LocalInstance instance = Instances.getInstance(UUID.fromString(data.uuid));
        if (instance == null) {
            Settings.webSocketAPI.sendMessage(new InstanceBackupsErrorReply(data, "Unable to locate modpack for backups"));
            return;
        }
        
        var backupsPath = instance.getDir().resolve("backups");
        var backupsJsonPath = backupsPath.resolve("backups.json");
            
        var emptyReply = new InstanceBackupsReply(data, List.of());
        if (!Files.exists(backupsJsonPath)) {
            Settings.webSocketAPI.sendMessage(emptyReply);
            return;
        }

        try {
            List<Backup> backups = GsonUtils.loadJson(backupsJsonPath, new TypeToken<List<Backup>>(){}.getType());
            Settings.webSocketAPI.sendMessage(new InstanceBackupsReply(data, backups));
        } catch (IOException e) {
            LOGGER.warn("Unable to read backups json...", e);
            Settings.webSocketAPI.sendMessage(emptyReply);
        }
    }

    public static class BackupsData extends BaseData {
        public String uuid;
    }
    
    private static class InstanceBackupsReply extends BackupsData {
        public List<Backup> backups;
        public InstanceBackupsReply(BackupsData data, List<Backup> backups) {
            this.uuid = data.uuid;
            this.type = data.type + "Reply";
            this.backups = backups;
        }
    } 
    
    private static class InstanceBackupsErrorReply extends BackupsData {
        public String message;

        public InstanceBackupsErrorReply(BackupsData data, String message) {
            this.uuid = data.uuid;
            this.type = data.type + "ErrorReply";
            this.message = message;
        }
    }

    private static class Backup {
        public String worldName = "";
        public long createTime = 0;
        public String backupLocation = "";
        public long size = 0;
        public float ratio = 0;
        public String sha1 = "";
        public String preview = "";
        public boolean snapshot = false;

        public Backup(String worldName, long createTime, String backupLocation, long size, float ratio, String sha1, String preview, boolean snapshot) {
            this.worldName = worldName;
            this.createTime = createTime;
            this.backupLocation = backupLocation;
            this.size = size;
            this.ratio = ratio;
            this.sha1 = sha1;
            this.preview = preview;
            this.snapshot = snapshot;
        }
    }
}
