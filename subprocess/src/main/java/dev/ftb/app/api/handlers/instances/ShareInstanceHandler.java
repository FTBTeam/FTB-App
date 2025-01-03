package dev.ftb.app.api.handlers.instances;

import dev.ftb.app.Instances;
import dev.ftb.app.accounts.AccountManager;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.instances.ShareInstanceData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.instance.InstanceSharer;
import dev.ftb.app.pack.Instance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;

public class ShareInstanceHandler implements IMessageHandler<ShareInstanceData> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(ShareInstanceData data) {
        if (AccountManager.get().getActiveProfile() == null) {
            WebSocketHandler.sendMessage(new ShareInstanceData.Reply(data, "error", "No profile selected.", data.uuid, ""));
            return;
        }

        CompletableFuture.runAsync(() -> {
            Instance instance = Instances.getInstance(data.uuid);
            try {
                InstanceSharer instanceSharer = new InstanceSharer(instance);
                // TODO We should probably pass some user configurable excludes?
                instanceSharer.prepare();

                String code = instanceSharer.execute();
                WebSocketHandler.sendMessage(new ShareInstanceData.Reply(data, "success", "", data.uuid, code));
            } catch (Throwable ex) {
                LOGGER.error("Failed to share instance.", ex);
                WebSocketHandler.sendMessage(new ShareInstanceData.Reply(data, "error", ex.getMessage(), data.uuid, ""));
            }
        });
    }
}
