package dev.ftb.app.api.data.instances;

import dev.ftb.app.api.data.BaseData;

import java.util.UUID;

public class SyncCloudInstanceData extends BaseData {

    public UUID uuid;

    public static class Reply extends BaseData {

        public final String status;
        public final String message;

        public Reply(String requestId, String status, String message) {
            type = "syncInstanceReply";
            this.requestId = requestId;
            this.status = status;
            this.message = message;
        }
    }
}
