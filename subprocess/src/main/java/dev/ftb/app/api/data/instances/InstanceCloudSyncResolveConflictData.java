package dev.ftb.app.api.data.instances;

import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.instance.cloud.CloudSyncOperation;

import java.util.UUID;

/**
 * Created by covers1624 on 1/9/23.
 */
public class InstanceCloudSyncResolveConflictData extends BaseData {

    public final UUID uuid;
    public final CloudSyncOperation.SyncDirection resolution;

    public InstanceCloudSyncResolveConflictData(UUID uuid, CloudSyncOperation.SyncDirection resolution) {
        this.uuid = uuid;
        this.resolution = resolution;
    }

    public static class Reply extends BaseData {

        public final String status;
        public final String message;

        public Reply(String status, String message) {
            type = "pollCloudInstancesReply";
            this.status = status;
            this.message = message;
        }
    }
}
