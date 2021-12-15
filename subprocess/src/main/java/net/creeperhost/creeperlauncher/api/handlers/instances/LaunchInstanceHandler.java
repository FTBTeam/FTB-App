package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.instances.LaunchInstanceData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.pack.InstanceLaunchException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

public class LaunchInstanceHandler implements IMessageHandler<LaunchInstanceData> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(LaunchInstanceData data) {
        String _uuid = data.uuid;
        UUID uuid = UUID.fromString(_uuid);
        try {
            Instances.getInstance(uuid).play(data.requestId, data.extraArgs, data.loadInApp);
            Settings.webSocketAPI.sendMessage(new LaunchInstanceData.Reply(data, "success", ""));
        } catch (InstanceLaunchException ex) {
            LOGGER.error("Failed to launch instance.", ex);

            String message = ex.getMessage();
            if (ex.getCause() != null) {
                message += " because.. " + ex.getCause().getMessage();
            }

            Settings.webSocketAPI.sendMessage(new LaunchInstanceData.Reply(data, "error", message));
        }
    }
}
