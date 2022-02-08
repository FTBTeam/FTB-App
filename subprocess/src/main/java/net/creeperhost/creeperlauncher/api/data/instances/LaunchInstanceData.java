package net.creeperhost.creeperlauncher.api.data.instances;

import net.creeperhost.creeperlauncher.api.data.BaseData;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class LaunchInstanceData extends BaseData {

    public String uuid;
    public String extraArgs = "";
    public boolean loadInApp = true;
    public boolean cancelLaunch = false;

    public static class Status extends BaseData {

        // The current step number (1-totalSteps)
        public int step;
        // The total number of steps that need to be executed.
        public int totalSteps;

        // Current step progress, between 0-1
        public float stepProgress;

        // Name for the step.
        public String stepDesc;

        // Human-readable step progress (xMB / yMB)
        @Nullable
        public String stepProgressHuman;

        public Status(int step, int totalSteps, float stepProgress, String stepDesc, @Nullable String stepProgressHuman) {
            this.type = "launchInstance.status";
            this.step = step;
            this.totalSteps = totalSteps;
            this.stepProgress = stepProgress;
            this.stepDesc = stepDesc;
            this.stepProgressHuman = stepProgressHuman;
        }
    }

    public static class Reply extends BaseData {

        public String status;

        public String message;

        public Reply(LaunchInstanceData data, String status, String message) {
            type = "launchInstance.reply";
            requestId = data.requestId;
            this.status = status;
            this.message = message;
        }
    }

    public static class Stopped extends BaseData {

        public UUID instanceId;
        public String status;

        public Stopped(UUID instanceId, String status) {
            this.instanceId = instanceId;
            this.status = status;
        }
    }

    public static class Logs extends BaseData {

        // The UUID of the instance.
        public final String uuid;
        // The log messages to append.
        public final List<String> messages;

        public Logs(UUID instanceId, List<String> messages) {
            type = "launchInstance.logs";
            this.uuid = instanceId.toString();
            this.messages = messages;
        }
    }
}
