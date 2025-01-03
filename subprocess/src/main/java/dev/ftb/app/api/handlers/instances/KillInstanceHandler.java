package dev.ftb.app.api.handlers.instances;

import dev.ftb.app.Instances;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.instances.KillInstanceData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.pack.Instance;
import dev.ftb.app.pack.InstanceLauncher;

public class KillInstanceHandler implements IMessageHandler<KillInstanceData> {

    @Override
    public void handle(KillInstanceData data) {
        Instance instance = Instances.getInstance(data.uuid);
        if (instance == null) {
            WebSocketHandler.sendMessage(new KillInstanceData.Reply(data, "error", "Instance does not exist"));
            return;
        }

        InstanceLauncher launcher = instance.getLauncher();

        if (!launcher.isRunning()) {
            WebSocketHandler.sendMessage(new KillInstanceData.Reply(data, "error", "Instance is not running"));
            return;
        }

        instance.forceStop();
        WebSocketHandler.sendMessage(new KillInstanceData.Reply(data, "success", "Instance force stopped"));
    }
}
