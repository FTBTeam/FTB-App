package net.creeperhost.creeperlauncher.api.data.instances;

import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.install.tasks.FTBModPackInstallerTask;

public class InstallInstanceData extends BaseData
{
    public static String typePrefix = "install";
    public String uuid;
    public long id;
    public long version;
    public boolean _private = false;
    public byte packType = 0;

    public static class Reply extends BaseData
    {
        final String status;
        final String message;
        final String uuid;

        public Reply(InstallInstanceData data, String status, String message, String uuid)
        {
            type = typePrefix + "InstanceDataReply";
            requestId = data.requestId;
            this.status = status;
            this.message = message;
            this.uuid = uuid;
            // Todo: get tasks from install, update corresponding TaskData objects and send update to other side
        }
    }

    public static class Progress extends BaseData
    {
        //final HashMap<Integer, TaskData> tasks;
        final Double overallPercentage;
        final long speed;
        final long currentBytes;
        final long overallBytes;
        final FTBModPackInstallerTask.Stage currentStage;

        public Progress(InstallInstanceData data, Double overallPercentage, long speed, long currentBytes, long overallBytes, FTBModPackInstallerTask.Stage currentStage)
        {
            this.requestId = data.requestId;
            type = typePrefix + "InstanceProgress";
            //this.tasks = tasks;
            // TODO: pass in tasks that have updated
            this.overallPercentage = overallPercentage;
            this.speed = speed;
            this.currentBytes = currentBytes;
            this.overallBytes = overallBytes;
            this.currentStage = currentStage;
        }
    }

    public static class TaskData
    {
        int id;
        String taskName;
        int progress;

        public TaskData(int id, String taskName, int progress)
        {
            this.id = id;
            this.taskName = taskName;
            this.progress = progress;
        }
    }
}
