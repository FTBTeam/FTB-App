package dev.ftb.app.api.handlers.instances;

import dev.ftb.app.Instances;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.instances.InstanceModToggleData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.pack.Instance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class InstanceModToggleHandler implements IMessageHandler<InstanceModToggleData> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(InstanceModToggleData data) {
        Instance instance = Instances.getInstance(data.uuid);
        if (instance == null) return;

        try {
            instance.toggleMod(data.fileId, data.fileName);
            WebSocketHandler.sendMessage(new InstanceModToggleData.Reply(data, true));
        } catch (Throwable ex) {
            LOGGER.warn("Error whilst toggling mod state.", ex);
            WebSocketHandler.sendMessage(new InstanceModToggleData.Reply(data, false));
        }
    }
}
