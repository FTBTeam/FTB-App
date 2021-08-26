package net.creeperhost.creeperlauncher.api.handlers.instances;

import com.google.gson.JsonObject;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.api.data.instances.InstalledInstancesData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.pack.LocalInstance;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class InstalledInstancesHandler implements IMessageHandler<InstalledInstancesData>
{

    @Override
    public void handle(InstalledInstancesData data)
    {
        int id = data.requestId;
        boolean refresh = data.refresh;
        CompletableFuture.runAsync(() -> {
            if(refresh) Instances.refreshInstances();
            List<LocalInstance> installedInstances;
            List<JsonObject> cloudInstances;
            try {
                //TODO, an exception will never be produced here.
                Instances.allInstances();
                Instances.cloudInstances();
            } catch(Throwable t)
            {
                Instances.refreshInstances();
            } finally
            {
                installedInstances = Instances.allInstances();
                cloudInstances = Instances.cloudInstances();
            }
            InstalledInstancesData.Reply reply = new InstalledInstancesData.Reply(id, installedInstances, cloudInstances);
            Settings.webSocketAPI.sendMessage(reply);
        });
    }
}
