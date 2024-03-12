package net.creeperhost.creeperlauncher.api.handlers.instances.backups;

import com.google.gson.JsonSyntaxException;
import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.api.WebSocketHandler;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.pack.Instance;
import net.creeperhost.creeperlauncher.util.GsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

public class InstanceGetBackupsHandler implements IMessageHandler<InstanceGetBackupsHandler.Request> {
    public static final Logger LOGGER = LogManager.getLogger();
    
    @Override
    public void handle(Request data) {
        Instance instance = Instances.getInstance(UUID.fromString(data.uuid));
        if (instance == null) {
            WebSocketHandler.sendMessage(new InstanceBackupsErrorReply(data, "Unable to locate modpack for backups"));
            return;
        }
        
        var backupsPath = instance.getDir().resolve("backups");
        var backupsJsonPath = backupsPath.resolve("backups.json");
            
        var emptyReply = new Reply(data, List.of());
        if (!Files.exists(backupsJsonPath)) {
            WebSocketHandler.sendMessage(emptyReply);
            return;
        }

        try {
            BackupsJson backups = GsonUtils.loadJson(backupsJsonPath, BackupsJson.class);
            WebSocketHandler.sendMessage(new Reply(data, backups.backups));
        } catch (IOException | JsonSyntaxException e) {
            LOGGER.warn("Unable to read backups json...", e);
            WebSocketHandler.sendMessage(emptyReply);
        }
    }

    public static class Request extends BaseData {
        public String uuid;
    }
    
    private static class Reply extends Request {
        public List<Backup> backups;
        public Reply(Request data, List<Backup> backups) {
            this.uuid = data.uuid;
            this.requestId = data.requestId;
            this.type = data.type + "Reply";
            this.backups = backups;
        }
    } 
    
    private static class InstanceBackupsErrorReply extends Request {
        public String message;

        public InstanceBackupsErrorReply(Request data, String message) {
            this.uuid = data.uuid;
            this.type = data.type + "ErrorReply";
            this.requestId = data.requestId;
            this.message = message;
        }
    }

    public static class BackupsJson {
        public List<Backup> backups;
    }
    
    public static class Backup {
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
