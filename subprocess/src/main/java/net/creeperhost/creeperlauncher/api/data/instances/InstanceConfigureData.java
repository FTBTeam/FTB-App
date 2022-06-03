package net.creeperhost.creeperlauncher.api.data.instances;

import net.creeperhost.creeperlauncher.api.data.BaseData;

import java.util.HashMap;
import java.util.UUID;

public class InstanceConfigureData extends BaseData
{
    public UUID uuid;
    public HashMap<String, String> instanceInfo; //TODO: second parameter should be something other than String maybe

    public static class Reply extends BaseData
    {
        UUID uuid;
        String status;

        public Reply(InstanceConfigureData data, String status)
        {
            type = "instanceConfigureReply";
            this.requestId = data.requestId;
            this.uuid = data.uuid;
            this.status = "success";
        }
    }
}
