package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.api.WebSocketHandler;
import net.creeperhost.creeperlauncher.api.data.instances.SyncCloudInstanceData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.pack.Instance;

public class SyncCloudInstanceHandler implements IMessageHandler<SyncCloudInstanceData> {

    @Override
    public void handle(SyncCloudInstanceData data) {
        if (!CreeperLauncher.CLOUD_SAVE_MANAGER.isConfigured()) {
            WebSocketHandler.sendMessage(new SyncCloudInstanceData.Reply(data.requestId, "error", "Cloud saves not configured."));
            return;
        }
        if (data.uuid == null) {
            WebSocketHandler.sendMessage(new SyncCloudInstanceData.Reply(data.requestId, "error", "Instance not specified."));
            return;
        }
        Instance instance = Instances.getInstance(data.uuid);
        if (instance == null) {
            WebSocketHandler.sendMessage(new SyncCloudInstanceData.Reply(data.requestId, "error", "Instance does not exist."));
            return;
        }

        if (!instance.props.cloudSaves) {
            WebSocketHandler.sendMessage(new SyncCloudInstanceData.Reply(data.requestId, "error", "Instance is not cloud enabled."));
            return;
        }

        if (CreeperLauncher.CLOUD_SAVE_MANAGER.isSyncing(instance.getUuid())) {
            WebSocketHandler.sendMessage(new SyncCloudInstanceData.Reply(data.requestId, "error", "Instance is already syncing."));
            return;
        }

        CreeperLauncher.CLOUD_SAVE_MANAGER.requestInstanceSync(instance);
        WebSocketHandler.sendMessage(new SyncCloudInstanceData.Reply(data.requestId, "success", "Instance sync queued."));
    }
}
