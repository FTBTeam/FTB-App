package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.api.data.instances.LaunchInstanceData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.pack.LocalInstance;

import java.util.UUID;

public class LaunchInstanceHandler implements IMessageHandler<LaunchInstanceData>
{
    @Override
    public void handle(LaunchInstanceData data)
    {
        String _uuid = data.uuid;
        UUID uuid = UUID.fromString(_uuid);
        LocalInstance instance = Instances.getInstance(uuid);
        instance.play(data.extraArgs, data.loadInApp);
        Settings.webSocketAPI.sendMessage(new LaunchInstanceData.Reply(data, "success"));
    }
}
