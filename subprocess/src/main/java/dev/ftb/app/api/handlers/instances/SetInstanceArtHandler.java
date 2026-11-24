package dev.ftb.app.api.handlers.instances;

import dev.ftb.app.api.data.instances.SetInstanceArtData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;
import dev.ftb.app.pack.Instance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SetInstanceArtHandler implements IMessageHandler<SetInstanceArtData> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(MessageContext context, SetInstanceArtData data) {
        Instance instance = context.services().instances().getInstance(data.uuid);
        if (instance == null) {
            context.reply(new SetInstanceArtData.Reply(data, "error", "Instance does not exist."));
            return;
        }

        Path path = Path.of(data.artPath);
        if (!Files.exists(path)) {
            context.reply(new SetInstanceArtData.Reply(data, "error", "Selected art file does not exist."));
            return;
        }

        if (!Files.isRegularFile(path)) {
            context.reply(new SetInstanceArtData.Reply(data, "error", "Selected art file is not a file."));
            return;
        }

        try {
            instance.updateArtwork(path);
        } catch (IOException ex) {
            LOGGER.warn("Failed to import art: {}", path, ex);
            context.reply(new SetInstanceArtData.Reply(data, "error", "Failed to import art: " + ex.getClass().getName() + ": " + ex.getMessage()));
            return;
        }
        context.reply(new SetInstanceArtData.Reply(data, "success", ""));
    }
}
