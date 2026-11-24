package dev.ftb.app.api.handlers.instances;

import dev.ftb.app.api.data.instances.InstanceModToggleData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;
import dev.ftb.app.pack.Instance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class InstanceModToggleHandler implements IMessageHandler<InstanceModToggleData> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(MessageContext context, InstanceModToggleData data) {
        Instance instance = context.services().instances().getInstance(data.uuid);
        if (instance == null) return;

        try {
            instance.toggleMod(data.fileId, data.fileName);
            context.reply(new InstanceModToggleData.Reply(data, true));
        } catch (Throwable ex) {
            LOGGER.warn("Error whilst toggling mod state.", ex);
            context.reply(new InstanceModToggleData.Reply(data, false));
        }
    }
}
