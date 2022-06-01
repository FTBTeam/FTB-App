package net.creeperhost.creeperlauncher.api.data.instances;

import net.creeperhost.creeperlauncher.api.data.BaseData;

public class CheckShareCodeData extends BaseData {
    public String shareCode;

    public static class Reply extends CheckShareCodeData {
        public boolean success;
        
        public Reply(CheckShareCodeData data, boolean success) {
            this.type = "CheckShareCodeDataReply";
            this.requestId = data.requestId;
            this.shareCode = data.shareCode;
            
            this.success = success;
        }
    }
}
