package net.creeperhost.creeperlauncher.api.data.instances;


import net.creeperhost.creeperlauncher.api.data.BaseData;
import org.jetbrains.annotations.Nullable;

public class BrowseInstanceData extends BaseData
{
    public String uuid;
    @Nullable public String folder;

    public static class Reply extends BaseData
    {
        String status;

        public Reply(BrowseInstanceData data, String status)
        {
            type = "browseInstanceReply";
            requestId = data.requestId;
            this.status = status;
        }
    }
}
