package net.creeperhost.creeperlauncher.api.data.instances;

import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.instances.InstalledInstancesHandler;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class InstanceConfigureData extends BaseData {
    public UUID uuid;
    public String instanceJson;
    
    public static class Reply extends BaseData {
        String errorMessage;
        String status;
        @Nullable InstalledInstancesHandler.SugaredInstanceJson instanceJson;

        public Reply(InstanceConfigureData data, String status, String errorMessage) {
            this(data, status, errorMessage, null);
        }

        public Reply(InstanceConfigureData data, String status, String errorMessage, @Nullable InstalledInstancesHandler.SugaredInstanceJson instanceJson) {
            type = "instanceConfigureReply";
            this.requestId = data.requestId;
            this.instanceJson = instanceJson;
            this.errorMessage = errorMessage;
            this.status = status;
        }
    }
}
