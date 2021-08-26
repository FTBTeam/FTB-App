package net.creeperhost.creeperlauncher.api.data.instances;

import net.creeperhost.creeperlauncher.install.tasks.FTBModPackInstallerTask;

public class UpdateInstanceData extends InstallInstanceData
{
    public static String typePrefix = "update";

    public static class Reply extends InstallInstanceData.Reply
    {

        public Reply(InstallInstanceData data, String status, String message, String uuid)
        {
            super(data, status, message, uuid);
        }
    }

    public static class Progress extends InstallInstanceData.Progress
    {

        public Progress(InstallInstanceData data, Double overallPercentage, long speed, long currentBytes, long overallBytes, FTBModPackInstallerTask.Stage currentStage)
        {
            super(data, overallPercentage, speed, currentBytes, overallBytes, currentStage);
        }
    }
}
