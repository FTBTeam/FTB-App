package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.api.WebSocketHandler;
import net.creeperhost.creeperlauncher.api.data.instances.SetInstanceArtData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.pack.Instance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by covers1624 on 1/6/22.
 */
public class SetInstanceArtHandler implements IMessageHandler<SetInstanceArtData> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(SetInstanceArtData data) {
        Instance instance = Instances.getInstance(data.uuid);
        if (instance == null) {
            WebSocketHandler.sendMessage(new SetInstanceArtData.Reply(data, "error", "Instance does not exist."));
            return;
        }

        Path path = Path.of(data.artPath);
        if (!Files.exists(path)) {
            WebSocketHandler.sendMessage(new SetInstanceArtData.Reply(data, "error", "Selected art file does not exist."));
            return;
        }

        if (!Files.isRegularFile(path)) {
            WebSocketHandler.sendMessage(new SetInstanceArtData.Reply(data, "error", "Selected art file is not a file."));
            return;
        }

        try {
            instance.importArt(path);
        } catch (IOException ex) {
            LOGGER.warn("Failed to import art: {}", path, ex);
            WebSocketHandler.sendMessage(new SetInstanceArtData.Reply(data, "error", "Failed to import art: " + ex.getClass().getName() + ": " + ex.getMessage()));
            return;
        }
        WebSocketHandler.sendMessage(new SetInstanceArtData.Reply(data, "success", ""));
    }
}
