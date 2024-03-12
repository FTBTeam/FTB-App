package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.api.WebSocketHandler;
import net.creeperhost.creeperlauncher.api.data.instances.InstanceModToggleData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.pack.Instance;
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
