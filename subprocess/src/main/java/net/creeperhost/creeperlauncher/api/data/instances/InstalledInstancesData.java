package net.creeperhost.creeperlauncher.api.data.instances;

import com.google.gson.JsonObject;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.instances.InstalledInstancesHandler;

import java.util.List;
import java.util.Set;

public class InstalledInstancesData extends BaseData {

    public boolean refresh = false;

    public static class Reply extends BaseData {

        List<InstalledInstancesHandler.SugaredInstanceJson> instances;
        List<JsonObject> cloudInstances;
        Set<String> availableCategories;

        public Reply(String requestId, List<InstalledInstancesHandler.SugaredInstanceJson> instances, List<JsonObject> cloudInstances, Set<String> availableCategories) {
            this.instances = instances;
            this.type = "installedInstancesReply";
            this.requestId = requestId;
            this.cloudInstances = cloudInstances;
            this.availableCategories = availableCategories;
        }
    }
}
