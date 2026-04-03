package dev.ftb.app.api.data.instances;

import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.handlers.instances.InstalledInstancesHandler;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public class InstanceConfigureData extends BaseData {
    public UUID uuid;
    public String instanceJson;
    
    public static class Reply extends BaseData {
        String errorMessage;
        String status;
        InstalledInstancesHandler.@Nullable SugaredInstanceJson instanceJson;

        public Reply(InstanceConfigureData data, String status, String errorMessage) {
            this(data, status, errorMessage, null);
        }

        public Reply(InstanceConfigureData data, String status, String errorMessage, InstalledInstancesHandler.@Nullable SugaredInstanceJson instanceJson) {
            type = "instanceConfigureReply";
            this.requestId = data.requestId;
            this.instanceJson = instanceJson;
            this.errorMessage = errorMessage;
            this.status = status;
        }
    }
}
