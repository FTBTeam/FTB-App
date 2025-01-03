package dev.ftb.app.api.handlers.instances;

import dev.ftb.app.AppMain;
import dev.ftb.app.Instances;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.instances.InstanceEnableCloudSavesData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.pack.Instance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class InstanceEnableCloudSavesHandler implements IMessageHandler<InstanceEnableCloudSavesData> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(InstanceEnableCloudSavesData data) {
        if (!AppMain.CLOUD_SAVE_MANAGER.isConfigured()) {
            WebSocketHandler.sendMessage(new InstanceEnableCloudSavesData.Reply(data, "error", "Cloud saves are not available."));
            return;
        }

        Instance instance = Instances.getInstance(data.instance);
        if (instance == null) {
            WebSocketHandler.sendMessage(new InstanceEnableCloudSavesData.Reply(data, "error", "Instance does not exist."));
            return;
        }

        try {
            instance.props.cloudSaves = true;
            instance.saveJson();
            AppMain.CLOUD_SAVE_MANAGER.requestInitialSync(instance);
            WebSocketHandler.sendMessage(new InstanceEnableCloudSavesData.Reply(data, "success", "Enabled!"));
        } catch (IOException ex) {
            LOGGER.error("Failed to disable cloud saves.", ex);
            WebSocketHandler.sendMessage(new InstanceEnableCloudSavesData.Reply(data, "error", "Failed to enable cloud saves. See logs."));
        }
    }
}
