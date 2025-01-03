package dev.ftb.app.api.data.instances;

import dev.ftb.app.api.data.BaseData;

import java.util.UUID;

public class InstanceCloudSyncConflictData extends BaseData {

    public final UUID uuid;
    public final String code;
    public final String message;

    public InstanceCloudSyncConflictData(UUID uuid, String code, String message) {
        type = "syncConflict";
        this.uuid = uuid;
        this.code = code;
        this.message = message;
    }
}
