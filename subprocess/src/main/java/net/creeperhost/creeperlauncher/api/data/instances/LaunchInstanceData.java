package net.creeperhost.creeperlauncher.api.data.instances;


import net.creeperhost.creeperlauncher.api.data.BaseData;

public class LaunchInstanceData extends BaseData
{
    public String uuid;
    public String extraArgs = "";
    public boolean loadInApp = true;

    public static class Reply extends BaseData
    {
        String status;

        public Reply(LaunchInstanceData data, String status)
        {
            type = "launchInstanceReply";
            requestId = data.requestId;
            this.status = status;
        }
    }
}
