package net.creeperhost.creeperlauncher.api.data.other;

import net.creeperhost.creeperlauncher.api.data.BaseData;

public class UploadLogsData extends BaseData {
    public String frontendLogs;
    public String uiVersion;
    public UploadLogsData()
    {
        type = "uploadLogs";
    }

    public static class Reply extends BaseData {
        private final boolean error;
        private String code;
        public Reply(int requestId, String code) {
            this.requestId = requestId;
            this.code = code;
            this.type = "uploadLogsReply";
            this.error = false;
        }

        public Reply(int requestId) {
            this.requestId = requestId;
            this.error = true;
        }
    }
}
