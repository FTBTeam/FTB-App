package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.api.WebSocketHandler;
import net.creeperhost.creeperlauncher.api.data.instances.PollCloudInstancesData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;

/**
 * Created by covers1624 on 15/6/23.
 */
public class PollCloudInstancesHandler implements IMessageHandler<PollCloudInstancesData> {

    @Override
    public void handle(PollCloudInstancesData data) {
        if (!CreeperLauncher.CLOUD_SAVE_MANAGER.isConfigured()) {
            WebSocketHandler.sendMessage(new PollCloudInstancesData.Reply("not_configured", "Cloud saves not configured."));
            return;
        }
        if (CreeperLauncher.CLOUD_SAVE_MANAGER.isCloudPollInProgress()) {
            WebSocketHandler.sendMessage(new PollCloudInstancesData.Reply("already_in_progress", "Cloud polling already in progress."));
            return;
        }

        CreeperLauncher.CLOUD_SAVE_MANAGER.pollCloudInstances().thenRunAsync(() -> {
            WebSocketHandler.sendMessage(new PollCloudInstancesData.Reply("done", "Finished."));
        });
    }
}
