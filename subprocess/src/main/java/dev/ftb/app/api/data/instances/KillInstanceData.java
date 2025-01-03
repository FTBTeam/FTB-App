package dev.ftb.app.api.data.instances;

import dev.ftb.app.api.data.BaseData;

import java.util.UUID;

public class KillInstanceData extends BaseData {

    public UUID uuid;

    public static class Reply extends BaseData {

        public final String status;
        public final String message;

        public Reply(KillInstanceData data, String status, String message) {
            requestId = data.requestId;
            type = "instance.kill.reply";
            this.status = status;
            this.message = message;
        }
    }
}
