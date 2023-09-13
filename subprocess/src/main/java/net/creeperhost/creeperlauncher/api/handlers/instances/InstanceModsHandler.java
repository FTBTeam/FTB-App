package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.instances.InstanceModsData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.pack.Instance;

public class InstanceModsHandler implements IMessageHandler<InstanceModsData> {

    @Override
    public void handle(InstanceModsData data) {
        Instance instance = Instances.getInstance(data.uuid);
        if (instance == null) return;

        Settings.webSocketAPI.sendMessage(new InstanceModsData.Reply(data, instance.getMods()));
    }
}
