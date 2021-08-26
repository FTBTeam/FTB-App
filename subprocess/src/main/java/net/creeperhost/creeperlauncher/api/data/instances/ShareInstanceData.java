package net.creeperhost.creeperlauncher.api.data.instances;

import net.creeperhost.creeperlauncher.api.data.BaseData;

public class ShareInstanceData extends BaseData
{
    public String uuid;

    public static class Reply extends BaseData
    {
        final String status;
        final String message;
        final String uuid;
        final String code;

        public Reply(ShareInstanceData data, String status, String message, String uuid, String code)
        {
            type = "ShareInstanceDataReply";
            requestId = data.requestId;
            this.status = status;
            this.message = message;
            this.uuid = uuid;
            this.code = code;
        }
    }
}
