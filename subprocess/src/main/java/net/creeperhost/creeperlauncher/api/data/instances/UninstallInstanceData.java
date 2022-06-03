package net.creeperhost.creeperlauncher.api.data.instances;

import net.creeperhost.creeperlauncher.api.data.BaseData;

import java.util.UUID;

public class UninstallInstanceData extends BaseData
{
    public UUID uuid;

    public static class Reply extends BaseData
    {
        final String status;
        final String message;

        public Reply(UninstallInstanceData data, String status, String message)
        {
            type = "uninstallInstanceDataReply";
            requestId = data.requestId;
            this.status = status;
            this.message = message;
        }
    }
}
