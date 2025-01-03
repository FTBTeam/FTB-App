package dev.ftb.app.api.data.instances;

import dev.ftb.app.api.data.BaseData;

import java.util.UUID;

public class InstanceDisableCloudSavesData extends BaseData {

    public UUID instance;

    public static class Reply extends BaseData {

        public final String status;
        public final String message;

        public Reply(InstanceDisableCloudSavesData data, String status, String message) {
            this.type = "instanceDisableCloudSavesReply";
            this.requestId = data.requestId;
            this.status = status;
            this.message = message;
        }
    }
}
