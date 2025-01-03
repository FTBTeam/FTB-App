package dev.ftb.app.api.data.instances;

import dev.ftb.app.api.data.BaseData;

import java.util.UUID;

public class ShareInstanceData extends BaseData {

    public UUID uuid;

    public static class Reply extends BaseData {

        public final String status;
        public final String message;
        public final UUID uuid;
        public final String code;

        public Reply(ShareInstanceData data, String status, String message, UUID uuid, String code) {
            type = "ShareInstanceDataReply";
            requestId = data.requestId;
            this.status = status;
            this.message = message;
            this.uuid = uuid;
            this.code = code;
        }
    }
}
