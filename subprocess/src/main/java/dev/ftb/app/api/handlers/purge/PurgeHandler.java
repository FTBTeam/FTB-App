package dev.ftb.app.api.handlers.purge;

import com.google.gson.annotations.JsonAdapter;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.util.FileUtils;
import net.covers1624.quack.gson.LowerCaseEnumAdapterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PurgeHandler implements IMessageHandler<PurgeHandler.Data> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PurgeHandler.class);
    
    @Override
    public void handle(Data data) {
        if (data.target == null) {
            WebSocketHandler.sendMessage(new Reply(data, "error", "No purge target specified."));
            return;
        }
        
        try {
            purgeTarget(data.target);
            WebSocketHandler.sendMessage(new Reply(data, "success", "Purge completed for target: " + data.target));
        } catch (Exception e) {
            WebSocketHandler.sendMessage(new Reply(data, "error", "Purge failed for target: " + data.target + " with error: " + e.getMessage()));
        }
    }
    
    private void purgeTarget(PurgeTarget target) {
        List<Path> paths = new ArrayList<>();
        if (target == PurgeTarget.ALL) {
            for (PurgeTarget t : PurgeTarget.values()) {
                if (t != PurgeTarget.ALL) {
                    paths.addAll(t.paths().get());
                }
            }
        } else {
            paths.addAll(target.paths().get());
        }
        
        // Go over all the paths given and delete the contents. If it's a dir, delete recursively.
        for (Path path : paths) {
            LOGGER.info("Purge target: {}", path.toString());
            FileUtils.deletePath(path);
        }
    }

    public static class Data extends BaseData {
        @JsonAdapter(LowerCaseEnumAdapterFactory.class)
        PurgeTarget target;
    }
    
    public static class Reply extends BaseData {
        String status;
        String message;
        
        public Reply(Data data, String status, String message) {
            this.requestId = data.requestId;
            this.type = data.type + "Reply";
            this.status = status;
            this.message = message;
        }
    }
}
