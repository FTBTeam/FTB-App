package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.instances.PollCloudInstancesData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;

/**
 * Created by covers1624 on 15/6/23.
 */
public class PollCloudInstancesHandler implements IMessageHandler<PollCloudInstancesData> {

    @Override
    public void handle(PollCloudInstancesData data) {
        if (!CreeperLauncher.CLOUD_SAVE_MANAGER.isConfigured()) {
            Settings.webSocketAPI.sendMessage(new PollCloudInstancesData.Reply("not_configured", "Cloud saves not configured."));
            return;
        }
        if (CreeperLauncher.CLOUD_SAVE_MANAGER.isCloudPollInProgress()) {
            Settings.webSocketAPI.sendMessage(new PollCloudInstancesData.Reply("already_in_progress", "Cloud polling already in progress."));
            return;
        }

        CreeperLauncher.CLOUD_SAVE_MANAGER.pollCloudInstances().thenRunAsync(() -> {
            Settings.webSocketAPI.sendMessage(new PollCloudInstancesData.Reply("done", "Finished."));
        });
    }
}
