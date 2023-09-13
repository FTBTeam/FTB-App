package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.instances.InstanceEnableCloudSavesData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.pack.Instance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Created by covers1624 on 12/9/23.
 */
public class InstanceEnableCloudSavesHandler implements IMessageHandler<InstanceEnableCloudSavesData> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(InstanceEnableCloudSavesData data) {
        if (!CreeperLauncher.CLOUD_SAVE_MANAGER.isConfigured()) {
            Settings.webSocketAPI.sendMessage(new InstanceEnableCloudSavesData.Reply(data, "error", "Cloud saves are not available."));
            return;
        }

        Instance instance = Instances.getInstance(data.instance);
        if (instance == null) {
            Settings.webSocketAPI.sendMessage(new InstanceEnableCloudSavesData.Reply(data, "error", "Instance does not exist."));
            return;
        }

        try {
            instance.props.cloudSaves = true;
            instance.saveJson();
            CreeperLauncher.CLOUD_SAVE_MANAGER.requestInitialSync(instance);
            Settings.webSocketAPI.sendMessage(new InstanceEnableCloudSavesData.Reply(data, "success", "Enabled!"));
        } catch (IOException ex) {
            LOGGER.error("Failed to disable cloud saves.", ex);
            Settings.webSocketAPI.sendMessage(new InstanceEnableCloudSavesData.Reply(data, "error", "Failed to enable cloud saves. See logs."));
        }
    }
}
