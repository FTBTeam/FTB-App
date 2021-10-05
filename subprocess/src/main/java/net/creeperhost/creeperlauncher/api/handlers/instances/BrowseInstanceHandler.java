package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.api.data.instances.BrowseInstanceData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.pack.LocalInstance;

import java.util.UUID;

public class BrowseInstanceHandler implements IMessageHandler<BrowseInstanceData>
{
    @Override
    public void handle(BrowseInstanceData data)
    {
        try
        {
            LocalInstance instance = Instances.getInstance(UUID.fromString(data.uuid));
            if (instance.browse())
            {
                Settings.webSocketAPI.sendMessage(new BrowseInstanceData.Reply(data, "success"));
            } else
            {
                Settings.webSocketAPI.sendMessage(new BrowseInstanceData.Reply(data, "error"));
            }
        } catch (Exception err)
        {
            Settings.webSocketAPI.sendMessage(new BrowseInstanceData.Reply(data, "error"));
        }
    }
}
