package net.creeperhost.creeperlauncher.api.data.instances;

import com.google.gson.JsonObject;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.instances.InstalledInstancesHandler;
import net.creeperhost.creeperlauncher.data.InstanceJson;

import java.util.List;

public class InstalledInstancesData extends BaseData {

    public boolean refresh = false;

    public static class Reply extends BaseData {

        List<InstalledInstancesHandler.SugaredInstanceJson> instances;
        List<JsonObject> cloudInstances;

        public Reply(String requestId, List<InstalledInstancesHandler.SugaredInstanceJson> instances, List<JsonObject> cloudInstances) {
            this.instances = instances;
            this.type = "installedInstancesReply";
            this.requestId = requestId;
            this.cloudInstances = cloudInstances;
        }
    }
}
