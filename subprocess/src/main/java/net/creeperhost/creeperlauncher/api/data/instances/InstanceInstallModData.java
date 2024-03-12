package net.creeperhost.creeperlauncher.api.data.instances;

import net.creeperhost.creeperlauncher.api.data.BaseData;

import java.util.List;
import java.util.UUID;

public class InstanceInstallModData extends BaseData {

    public UUID uuid;
    public long modId;
    public long versionId;

    public static class Reply extends BaseData {

        public final String status;
        public final String message;
        public final List<PendingInstall> dependencies;

        public Reply(InstanceInstallModData data, String status, String message) {
            this(data, status, message, List.of());
        }

        public Reply(InstanceInstallModData data, String status, String message, List<PendingInstall> dependencies) {
            type = "instanceInstallModReply";
            requestId = data.requestId;
            this.status = status;
            this.message = message;
            this.dependencies = dependencies;
        }
    }

    public record PendingInstall(long modId, long versionId) {
    }
}
