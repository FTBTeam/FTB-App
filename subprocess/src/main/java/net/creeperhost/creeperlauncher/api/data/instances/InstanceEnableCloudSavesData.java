package net.creeperhost.creeperlauncher.api.data.instances;

import net.creeperhost.creeperlauncher.api.data.BaseData;

import java.util.UUID;

/**
 * Created by covers1624 on 12/9/23.
 */
public class InstanceEnableCloudSavesData extends BaseData {

    public UUID instance;

    public static class Reply extends BaseData {

        public final String status;
        public final String message;

        public Reply(InstanceEnableCloudSavesData data, String status, String message) {
            this.type = "instanceEnableCloudSavesReply";
            this.requestId = data.requestId;
            this.status = status;
            this.message = message;
        }
    }
}
