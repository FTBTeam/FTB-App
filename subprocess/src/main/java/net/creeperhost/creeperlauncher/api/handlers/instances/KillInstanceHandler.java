package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.instances.KillInstanceData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.pack.InstanceLauncher;
import net.creeperhost.creeperlauncher.pack.Instance;

/**
 * Created by covers1624 on 9/2/22.
 */
public class KillInstanceHandler implements IMessageHandler<KillInstanceData> {

    @Override
    public void handle(KillInstanceData data) {
        Instance instance = Instances.getInstance(data.uuid);
        if (instance == null) {
            Settings.webSocketAPI.sendMessage(new KillInstanceData.Reply(data, "error", "Instance does not exist"));
            return;
        }

        InstanceLauncher launcher = instance.getLauncher();

        if (!launcher.isRunning()) {
            Settings.webSocketAPI.sendMessage(new KillInstanceData.Reply(data, "error", "Instance is not running"));
            return;
        }

        instance.forceStop();
        Settings.webSocketAPI.sendMessage(new KillInstanceData.Reply(data, "success", "Instance force stopped"));
    }
}
