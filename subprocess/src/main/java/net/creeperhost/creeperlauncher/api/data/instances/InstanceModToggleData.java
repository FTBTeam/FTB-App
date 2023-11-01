package net.creeperhost.creeperlauncher.api.data.instances;

import net.creeperhost.creeperlauncher.api.data.BaseData;

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
