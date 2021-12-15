package net.creeperhost.creeperlauncher.api.data.instances;

import net.creeperhost.creeperlauncher.api.data.BaseData;
import org.jetbrains.annotations.Nullable;

public class LaunchInstanceData extends BaseData {

    public String uuid;
    public String extraArgs = "";
    public boolean loadInApp = true;

    public static class Status extends BaseData {

        public int step;
        public int totalSteps;

        // Value between 0-1.
        public float stepProgress;

        public String stepDesc;

        @Nullable
        public String stepProgressHuman;

        public Status(int requestId, int step, int totalSteps, float stepProgress, String stepDesc, @Nullable String stepProgressHuman) {
            this.type = "launchInstance.status";
            this.requestId = requestId;
            this.step = step;
            this.totalSteps = totalSteps;
            this.stepProgress = stepProgress;
            this.stepDesc = stepDesc;
            this.stepProgressHuman = stepProgressHuman;
        }
    }

    public static class Reply extends BaseData {

        public String status;

        public Reply(LaunchInstanceData data, String status) {
            // TODO this name needs to be changed.
            type = "launchInstanceReply";
            requestId = data.requestId;
            this.status = status;
        }
    }
}
