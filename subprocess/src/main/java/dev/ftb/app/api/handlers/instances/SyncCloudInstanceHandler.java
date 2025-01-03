package dev.ftb.app.api.handlers.instances;

import dev.ftb.app.AppMain;
import dev.ftb.app.Instances;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.instances.SyncCloudInstanceData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.pack.Instance;

public class SyncCloudInstanceHandler implements IMessageHandler<SyncCloudInstanceData> {

    @Override
    public void handle(SyncCloudInstanceData data) {
        if (!AppMain.CLOUD_SAVE_MANAGER.isConfigured()) {
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

        if (AppMain.CLOUD_SAVE_MANAGER.isSyncing(instance.getUuid())) {
            WebSocketHandler.sendMessage(new SyncCloudInstanceData.Reply(data.requestId, "error", "Instance is already syncing."));
            return;
        }

        AppMain.CLOUD_SAVE_MANAGER.requestInstanceSync(instance);
        WebSocketHandler.sendMessage(new SyncCloudInstanceData.Reply(data.requestId, "success", "Instance sync queued."));
    }
}
