package net.creeperhost.creeperlauncher.api.data.instances;

import net.creeperhost.creeperlauncher.api.data.BaseData;

public class CancelInstallInstanceData extends BaseData
{
    public String uuid;

    public static class Reply extends BaseData
    {
        final String status;
        final String message;
        final String uuid;

        public Reply(CancelInstallInstanceData data, String status, String message, String uuid)
        {
            type = "installInstanceDataReply";
            requestId = data.requestId;
            this.status = status;
            this.message = message;
            this.uuid = uuid;
            // Todo: get tasks from install, update corresponding TaskData objects and send update to other side
        }
    }
}
