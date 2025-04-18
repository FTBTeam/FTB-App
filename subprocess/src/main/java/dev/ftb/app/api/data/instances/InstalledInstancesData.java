package dev.ftb.app.api.data.instances;

import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.handlers.instances.InstalledInstancesHandler;

import java.util.List;
import java.util.Set;

public class InstalledInstancesData extends BaseData {
    public boolean refresh = false;

    public static class Reply extends BaseData {
        List<InstalledInstancesHandler.SugaredInstanceJson> instances;
        Set<String> availableCategories;

        public Reply(String requestId, List<InstalledInstancesHandler.SugaredInstanceJson> instances, Set<String> availableCategories) {
            this.instances = instances;
            this.type = "installedInstancesReply";
            this.requestId = requestId;
            this.availableCategories = availableCategories;
        }
    }
}
