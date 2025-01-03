package dev.ftb.app.api.data.instances;

import dev.ftb.app.api.data.BaseData;

import java.util.UUID;

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
