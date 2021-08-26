package net.creeperhost.creeperlauncher.api.data.instances;

import com.google.gson.JsonObject;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.pack.LocalInstance;

import java.util.List;

public class InstalledInstancesData extends BaseData
{
    public boolean refresh = false;
    public static class Reply extends BaseData
    {
        List<LocalInstance> instances;
        List<JsonObject> cloudInstances;

        public Reply(int requestId, List<LocalInstance> instances, List<JsonObject> cloudInstances)
        {
            this.instances = instances;
            this.type = "installedInstancesReply";
            this.requestId = requestId;
            this.cloudInstances = cloudInstances;
        }
    }
}
