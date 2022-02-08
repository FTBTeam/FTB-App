package net.creeperhost.creeperlauncher.api.data.instances;

import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.install.InstallProgressTracker.InstallStage;

import java.util.Map;

public class InstallInstanceData extends BaseData {

    public static final String typePrefix = "install";

    public String uuid;
    public long id;
    public long version;
    public boolean _private = false;
    public byte packType = 0;

    public static class Reply extends BaseData {

        public final String status;
        public final String message;
        public final String uuid;

        public Reply(InstallInstanceData data, String status, String message, String uuid) {
            type = typePrefix + "InstanceDataReply";
            requestId = data.requestId;
            this.status = status;
            this.message = message;
            this.uuid = uuid;
        }
    }

    public static class Progress extends BaseData {

        public final double overallPercentage;
        public final long speed;
        public final long currentBytes;
        public final long overallBytes;
        public final InstallStage currentStage;

        public Progress(InstallInstanceData data, Double overallPercentage, long speed, long currentBytes, long overallBytes, InstallStage currentStage) {
            this.requestId = data.requestId;
            type = typePrefix + "InstanceProgress";
            this.overallPercentage = overallPercentage;
            this.speed = speed;
            this.currentBytes = currentBytes;
            this.overallBytes = overallBytes;
            this.currentStage = currentStage;
        }
    }

    public static class FilesEvent extends BaseData {

        public final Map<Long, String> files;

        public FilesEvent(Map<Long, String> files) {
            type = "install.filesEvent";
            this.files = files;
        }
    }
}
