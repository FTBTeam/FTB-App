package net.creeperhost.creeperlauncher.api.data.instances;

import net.creeperhost.creeperlauncher.api.data.BaseData;

import java.util.HashMap;

public class InstanceConfigureData extends BaseData
{
    public String uuid;
    public HashMap<String, String> instanceInfo; //TODO: second parameter should be something other than String maybe

    public static class Reply extends BaseData
    {
        String uuid;
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
