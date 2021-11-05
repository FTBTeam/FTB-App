package net.creeperhost.creeperlauncher.api.data.instances;

import net.creeperhost.creeperlauncher.api.data.BaseData;

public class InstanceModToggleData extends BaseData {
    public String uuid;
    public boolean state;
    public String fileName;

    public static class Reply extends InstanceModToggleData {
        public boolean successful;

        public Reply(InstanceModToggleData data, boolean successful, boolean state) {
            this.type = "instanceModToggleReply";
            this.requestId = data.requestId;
            this.successful = successful;
            this.state = state;
        }
    }
}
