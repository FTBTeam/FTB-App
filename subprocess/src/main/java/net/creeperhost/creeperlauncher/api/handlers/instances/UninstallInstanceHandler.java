package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.api.WebSocketHandler;
import net.creeperhost.creeperlauncher.api.data.instances.UninstallInstanceData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.pack.Instance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UninstallInstanceHandler implements IMessageHandler<UninstallInstanceData>
{
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(UninstallInstanceData data)
    {
        try
        {
            Instance instance = Instances.getInstance(data.uuid);
            if (instance != null) {
                instance.uninstall();
                WebSocketHandler.sendMessage(new UninstallInstanceData.Reply(data, "success", ""));
            } else {
                WebSocketHandler.sendMessage(new UninstallInstanceData.Reply(data, "error", "Instance does not exist."));
            }
        } catch (Exception err)
        {
            LOGGER.error("Error uninstalling pack", err);
            WebSocketHandler.sendMessage(new UninstallInstanceData.Reply(data, "error", err.toString()));
        }

    }
}
