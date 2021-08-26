package net.creeperhost.creeperlauncher.api.data.instances;

import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.ModFile;

import java.util.List;

public class InstanceModsData extends BaseData
{
    public String uuid;
    public boolean _private = false;
    public static class Reply extends InstanceModsData
    {
        public List<ModFile> files;

        public Reply(InstanceModsData data, List<ModFile> simpleDownloadableFiles)
        {
            type = "instanceModsReply";
            uuid = data.uuid;
            requestId = data.requestId;
            this.files = simpleDownloadableFiles;
        }
    }
}
