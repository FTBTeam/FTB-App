package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.covers1624.quack.collection.ColUtils;
import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.instances.SyncCloudInstanceData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.pack.Instance;

public class SyncCloudInstanceHandler implements IMessageHandler<SyncCloudInstanceData> {

    @Override
    public void handle(SyncCloudInstanceData data) {
        if (CreeperLauncher.CLOUD_SAVE_MANAGER.isConfigured()) {
            Settings.webSocketAPI.sendMessage(new SyncCloudInstanceData.Reply(data.requestId, "error", "Cloud saves not configured."));
            return;
        }
        if (data.uuid == null) {
            Settings.webSocketAPI.sendMessage(new SyncCloudInstanceData.Reply(data.requestId, "error", "Instance not specified."));
            return;
        }
        Instance instance = Instances.getInstance(data.uuid);
        if (instance == null) {
            Settings.webSocketAPI.sendMessage(new SyncCloudInstanceData.Reply(data.requestId, "error", "Instance does not exist."));
            return;
        }

        if (!instance.props.cloudSaves) {
            Settings.webSocketAPI.sendMessage(new SyncCloudInstanceData.Reply(data.requestId, "error", "Instance is not cloud enabled."));
            return;
        }

        if (ColUtils.anyMatch(CreeperLauncher.CLOUD_SAVE_MANAGER.getSyncOperations(), e -> e.operation.instance == instance)) {
            Settings.webSocketAPI.sendMessage(new SyncCloudInstanceData.Reply(data.requestId, "error", "Instance is already syncing."));
            return;
        }

        CreeperLauncher.CLOUD_SAVE_MANAGER.requestInstanceSync(instance);
        Settings.webSocketAPI.sendMessage(new SyncCloudInstanceData.Reply(data.requestId, "success", "Instance sync queued."));
    }
}
