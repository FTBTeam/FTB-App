package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.instances.ShareInstanceData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.instance.InstanceSharer;
import net.creeperhost.creeperlauncher.pack.LocalInstance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;

public class ShareInstanceHandler implements IMessageHandler<ShareInstanceData> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(ShareInstanceData data) {
        CompletableFuture.runAsync(() -> {
            LocalInstance instance = Instances.getInstance(data.uuid);
            try {
                InstanceSharer instanceSharer = new InstanceSharer(instance);
                // TODO We should probably pass some user configurable excludes?
                instanceSharer.prepare();

                String code = instanceSharer.execute();
                Settings.webSocketAPI.sendMessage(new ShareInstanceData.Reply(data, "success", "", data.uuid, code));
            } catch (Throwable ex) {
                LOGGER.error("Failed to share instance.", ex);
                Settings.webSocketAPI.sendMessage(new ShareInstanceData.Reply(data, "error", ex.getMessage(), data.uuid, ""));
            }
        });
    }
}
