package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.instances.BrowseInstanceData;
import net.creeperhost.creeperlauncher.api.data.instances.ShareInstanceData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.pack.LocalInstance;
import net.creeperhost.creeperlauncher.share.InstanceData;
import net.creeperhost.creeperlauncher.util.GsonUtils;
import net.creeperhost.creeperlauncher.util.WebUtils;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ShareInstanceHandler implements IMessageHandler<ShareInstanceData>
{
    @Override
    public void handle(ShareInstanceData data)
    {
        try
        {
            CompletableFuture.runAsync(() ->
            {
                LocalInstance instance = Instances.getInstance(UUID.fromString(data.uuid));
                try
                {
                    InstanceData instanceData = new InstanceData(instance);
                    String code = instanceData.share();

                    String URL = "https://api.modpacks.ch/" + Constants.KEY + "/modpack/share/" + code;
                    String json = GsonUtils.GSON.toJson(instanceData);
                    WebUtils.putWebResponse(URL, json, true, false);

                    Settings.webSocketAPI.sendMessage(new ShareInstanceData.Reply(data, "success", "", data.uuid, code));
                } catch (IOException e) { e.printStackTrace(); }
            });
        } catch (Exception err)
        {
            Settings.webSocketAPI.sendMessage(new ShareInstanceData.Reply(data, "error", "", data.uuid, ""));
        }
    }
}
