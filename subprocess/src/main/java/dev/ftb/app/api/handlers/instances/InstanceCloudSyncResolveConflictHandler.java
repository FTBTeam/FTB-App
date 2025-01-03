package dev.ftb.app.api.handlers.instances;

import dev.ftb.app.AppMain;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.instances.InstanceCloudSyncResolveConflictData;
import dev.ftb.app.api.handlers.IMessageHandler;

public class InstanceCloudSyncResolveConflictHandler implements IMessageHandler<InstanceCloudSyncResolveConflictData> {

    @Override
    public void handle(InstanceCloudSyncResolveConflictData data) {
        if (!AppMain.CLOUD_SAVE_MANAGER.isConfigured()) {
            WebSocketHandler.sendMessage(new InstanceCloudSyncResolveConflictData.Reply("not_configured", "Cloud saves not configured."));
            return;
        }

        try {
            AppMain.CLOUD_SAVE_MANAGER.resolveConflict(data.uuid, data.resolution);
            WebSocketHandler.sendMessage(new InstanceCloudSyncResolveConflictData.Reply("success", null));
        } catch (IllegalStateException ex) {
            WebSocketHandler.sendMessage(new InstanceCloudSyncResolveConflictData.Reply("error", ex.getMessage()));
        }
    }
}
