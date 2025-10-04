package dev.ftb.app.api.data.instances;

import dev.ftb.app.api.data.BaseData;

import java.util.UUID;

public class InstanceModToggleData extends BaseData {
    public UUID uuid;

    public long fileId;
    public String fileName;

    public static class Reply extends InstanceModToggleData {

        public boolean successful;

        public Reply(InstanceModToggleData data, boolean successful) {
            this.type = "instanceModToggleReply";
            this.requestId = data.requestId;
            this.successful = successful;
        }
    }
}
