package dev.ftb.app.api.handlers.instances;

import dev.ftb.app.AppMain;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.instances.PollCloudInstancesData;
import dev.ftb.app.api.handlers.IMessageHandler;

/**
 * Created by covers1624 on 15/6/23.
 */
public class PollCloudInstancesHandler implements IMessageHandler<PollCloudInstancesData> {

    @Override
    public void handle(PollCloudInstancesData data) {
        if (!AppMain.CLOUD_SAVE_MANAGER.isConfigured()) {
            WebSocketHandler.sendMessage(new PollCloudInstancesData.Reply("not_configured", "Cloud saves not configured."));
            return;
        }
        if (AppMain.CLOUD_SAVE_MANAGER.isCloudPollInProgress()) {
            WebSocketHandler.sendMessage(new PollCloudInstancesData.Reply("already_in_progress", "Cloud polling already in progress."));
            return;
        }

        AppMain.CLOUD_SAVE_MANAGER.pollCloudInstances().thenRunAsync(() -> {
            WebSocketHandler.sendMessage(new PollCloudInstancesData.Reply("done", "Finished."));
        });
    }
}
