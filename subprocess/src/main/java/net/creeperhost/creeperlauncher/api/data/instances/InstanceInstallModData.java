package net.creeperhost.creeperlauncher.api.data.instances;

import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.mod.Mod;

import java.util.List;

public class InstanceInstallModData extends BaseData {
    public String uuid;
    public int modId;
    public int versionId;

    public static class Reply extends BaseData
    {
        final String status;
        final String message;
        final List<Mod.Version> dependencyList;

        public Reply(InstanceInstallModData data, String status, String message, List<Mod.Version> dependencies)
        {
            type = "instanceInstallModReply";
            requestId = data.requestId;
            this.status = status;
            this.message = message;
            this.dependencyList = dependencies;
        }
    }

    public static class Progress extends BaseData {
        final Double overallPercentage;
        final long speed;
        final long currentBytes;
        final long overallBytes;

        public Progress(InstanceInstallModData data, Double overallPercentage, long speed, long currentBytes, long overallBytes) {
            this.requestId = data.requestId;
            type = "instanceInstallModProgress";
            this.overallPercentage = overallPercentage;
            this.speed = speed;
            this.currentBytes = currentBytes;
            this.overallBytes = overallBytes;
        }

    }
}
