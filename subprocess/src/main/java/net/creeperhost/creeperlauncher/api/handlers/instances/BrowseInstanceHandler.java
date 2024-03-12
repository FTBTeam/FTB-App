package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.api.WebSocketHandler;
import net.creeperhost.creeperlauncher.api.data.instances.BrowseInstanceData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.pack.Instance;

import java.util.UUID;

public class BrowseInstanceHandler implements IMessageHandler<BrowseInstanceData>
{
    @Override
    public void handle(BrowseInstanceData data)
    {
        try
        {
            Instance instance = Instances.getInstance(UUID.fromString(data.uuid));
            var success = data.folder != null ? instance.browse(data.folder) : instance.browse();
            if (success)
            {
                WebSocketHandler.sendMessage(new BrowseInstanceData.Reply(data, "success"));
            } else
            {
                WebSocketHandler.sendMessage(new BrowseInstanceData.Reply(data, "error"));
            }
        } catch (Exception err)
        {
            WebSocketHandler.sendMessage(new BrowseInstanceData.Reply(data, "error"));
        }
    }
}
