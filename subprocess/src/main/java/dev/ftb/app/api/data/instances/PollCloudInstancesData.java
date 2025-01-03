package dev.ftb.app.api.data.instances;

import dev.ftb.app.api.data.BaseData;

public class PollCloudInstancesData extends BaseData {

    public static class Reply extends BaseData {

        public final String status;
        public final String message;

        public Reply(String status, String message) {
            type = "pollCloudInstancesReply";
            this.status = status;
            this.message = message;
        }
    }
}
