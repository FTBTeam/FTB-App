package dev.ftb.app.api.handlers.instances;

import dev.ftb.app.Instances;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.instances.BrowseInstanceData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.pack.Instance;

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
