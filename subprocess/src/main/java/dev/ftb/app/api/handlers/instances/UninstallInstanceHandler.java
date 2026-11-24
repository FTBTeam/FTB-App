package dev.ftb.app.api.handlers.instances;

import dev.ftb.app.api.data.instances.UninstallInstanceData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;
import dev.ftb.app.pack.Instance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UninstallInstanceHandler implements IMessageHandler<UninstallInstanceData> {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(MessageContext context, UninstallInstanceData data) {
        try {
            Instance instance = context.services().instances().getInstance(data.uuid);
            if (instance != null) {
                instance.uninstall();
                context.reply(new UninstallInstanceData.Reply(data, "success", ""));
            } else {
                context.reply(new UninstallInstanceData.Reply(data, "error", "Instance does not exist."));
            }
        } catch (Exception err)
        {
            LOGGER.error("Error uninstalling pack", err);
            context.reply(new UninstallInstanceData.Reply(data, "error", err.toString()));
        }

    }
}
