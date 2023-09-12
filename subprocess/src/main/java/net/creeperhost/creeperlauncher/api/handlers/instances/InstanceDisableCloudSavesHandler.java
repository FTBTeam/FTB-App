package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.instances.InstanceDisableCloudSavesData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.pack.Instance;
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
        if (!CreeperLauncher.CLOUD_SAVE_MANAGER.isConfigured()) {
            Settings.webSocketAPI.sendMessage(new InstanceDisableCloudSavesData.Reply(data, "error", "Cloud saves are not available."));
            return;
        }

        Instance instance = Instances.getInstance(data.instance);
        if (instance == null) {
            Settings.webSocketAPI.sendMessage(new InstanceDisableCloudSavesData.Reply(data, "error", "Instance does not exist."));
            return;
        }

        CompletableFuture<Void> future = CreeperLauncher.CLOUD_SAVE_MANAGER.requestDisableSync(instance);
        if (future == null) {
            // TODO we can be smarter here and abort the sync.
            Settings.webSocketAPI.sendMessage(new InstanceDisableCloudSavesData.Reply(data, "error", "Sync is in progress. Please wait for it to finish."));
            return;
        }
        future.thenRun(() -> {
            try {
                instance.props.cloudSaves = false;
                instance.saveJson();
                Settings.webSocketAPI.sendMessage(new InstanceDisableCloudSavesData.Reply(data, "success", "Disabled!"));
            } catch (IOException ex) {
                LOGGER.error("Failed to save instance json.", ex);
                Settings.webSocketAPI.sendMessage(new InstanceDisableCloudSavesData.Reply(data, "error", "Failed to disable cloud saves. See logs."));
            }
        });

    }
}
