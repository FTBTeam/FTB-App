package dev.ftb.app.api.handlers.instances;

import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;
import dev.ftb.app.pack.Instance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class GetInstanceFoldersHandler implements IMessageHandler<GetInstanceFoldersHandler.Request> {

    private static final Logger LOGGER = LogManager.getLogger();
    @Override
    public void handle(MessageContext context, Request data) {
        Instance instance = context.services().instances().getInstance(UUID.fromString(data.uuid));
        if (instance == null) {
            context.reply(new Reply(data, false, Set.of()));
            return;
        }

        Path instanceDir = instance.getDir();
        try (var files = Files.walk(instanceDir, 1)) {
            var folders = files
                .filter(Files::isDirectory)
                .filter(e -> e != instanceDir)
                .map(e -> instanceDir.relativize(e).toString())
                .collect(Collectors.toSet());
            
            context.reply(new Reply(data, true, folders));
        } catch (IOException e) {
            LOGGER.error("Unable to read paths from {}", instanceDir, e);
            context.reply(new Reply(data, false, Set.of()));
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

