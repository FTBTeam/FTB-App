package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.instances.UninstallInstanceData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.pack.LocalInstance;
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
            //TODO, Instance lookup?
            LocalInstance instance = new LocalInstance(Settings.getInstanceLocOr(Constants.INSTANCES_FOLDER_LOC).resolve(data.uuid));
            instance.uninstall();
            Settings.webSocketAPI.sendMessage(new UninstallInstanceData.Reply(data, "success", ""));
        } catch (Exception err)
        {
            LOGGER.error("Error uninstalling pack", err);
            Settings.webSocketAPI.sendMessage(new UninstallInstanceData.Reply(data, "error", err.toString()));
        }

    }
}
