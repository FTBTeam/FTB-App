package dev.ftb.app.api.handlers.instances;

import dev.ftb.app.AppMain;
import dev.ftb.app.Instances;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.instances.InstanceDisableCloudSavesData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.pack.Instance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Created by covers1624 on 12/9/23.
 */
public class InstanceDisableCloudSavesHandler implements IMessageHandler<InstanceDisableCloudSavesData> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(InstanceDisableCloudSavesData data) {
        if (!AppMain.CLOUD_SAVE_MANAGER.isConfigured()) {
            WebSocketHandler.sendMessage(new InstanceDisableCloudSavesData.Reply(data, "error", "Cloud saves are not available."));
            return;
        }

        Instance instance = Instances.getInstance(data.instance);
        if (instance == null) {
            WebSocketHandler.sendMessage(new InstanceDisableCloudSavesData.Reply(data, "error", "Instance does not exist."));
            return;
        }

        CompletableFuture<Void> future = AppMain.CLOUD_SAVE_MANAGER.requestDisableSync(instance);
        if (future == null) {
            // TODO we can be smarter here and abort the sync.
            WebSocketHandler.sendMessage(new InstanceDisableCloudSavesData.Reply(data, "error", "Sync is in progress. Please wait for it to finish."));
            return;
        }
        future.thenRun(() -> {
            try {
                instance.props.cloudSaves = false;
                instance.saveJson();
                WebSocketHandler.sendMessage(new InstanceDisableCloudSavesData.Reply(data, "success", "Disabled!"));
            } catch (IOException ex) {
                LOGGER.error("Failed to save instance json.", ex);
                WebSocketHandler.sendMessage(new InstanceDisableCloudSavesData.Reply(data, "error", "Failed to disable cloud saves. See logs."));
            }
        });

    }
}
