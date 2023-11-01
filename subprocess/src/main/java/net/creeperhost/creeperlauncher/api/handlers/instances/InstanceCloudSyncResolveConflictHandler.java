package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.instances.InstanceCloudSyncResolveConflictData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;

/**
 * Created by covers1624 on 1/9/23.
 */
public class InstanceCloudSyncResolveConflictHandler implements IMessageHandler<InstanceCloudSyncResolveConflictData> {

    @Override
    public void handle(InstanceCloudSyncResolveConflictData data) {
        if (!CreeperLauncher.CLOUD_SAVE_MANAGER.isConfigured()) {
            Settings.webSocketAPI.sendMessage(new InstanceCloudSyncResolveConflictData.Reply("not_configured", "Cloud saves not configured."));
            return;
        }

        try {
            CreeperLauncher.CLOUD_SAVE_MANAGER.resolveConflict(data.uuid, data.resolution);
            Settings.webSocketAPI.sendMessage(new InstanceCloudSyncResolveConflictData.Reply("success", null));
        } catch (IllegalStateException ex) {
            Settings.webSocketAPI.sendMessage(new InstanceCloudSyncResolveConflictData.Reply("error", ex.getMessage()));
        }
    }
}
