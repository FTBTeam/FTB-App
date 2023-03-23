package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.WebSocketAPI;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.pack.Instance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class GetInstanceFoldersHandler implements IMessageHandler<GetInstanceFoldersHandler.Request> {
    @Override
    public void handle(Request data) {
        Instance instance = Instances.getInstance(UUID.fromString(data.uuid));
        if (instance == null) {
            Settings.webSocketAPI.sendMessage(new Reply(data, false, Set.of()));
            return;
        }

        Path instanceDir = instance.getDir();
        try (var files = Files.walk(instanceDir, 1)) {
            var folders = files
                .filter(Files::isDirectory)
                .filter(e -> e != instanceDir)
                .map(e -> instanceDir.relativize(e).toString())
                .collect(Collectors.toSet());
            
            Settings.webSocketAPI.sendMessage(new Reply(data, true, folders));
        } catch (IOException e) {
            WebSocketAPI.LOGGER.error("Unable to read paths from {}", instanceDir, e);
            Settings.webSocketAPI.sendMessage(new Reply(data, false, Set.of()));
        }
    }
    
    public static class Request extends BaseData {
        public String uuid;
    }
    
    private static class Reply extends Request {
        public Set<String> folders;
        public boolean success;

        public Reply(Request data, boolean success, Set<String> folders) {
            this.requestId = data.requestId;
            this.uuid = data.uuid;
            this.type = data.type + "Reply";
            this.folders = folders;
            this.success = success;
        }
    } 
}

