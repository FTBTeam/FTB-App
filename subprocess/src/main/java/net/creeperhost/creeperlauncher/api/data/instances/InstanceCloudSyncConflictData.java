package net.creeperhost.creeperlauncher.api.data.instances;

import net.creeperhost.creeperlauncher.api.data.BaseData;

import java.util.UUID;

/**
 * Created by covers1624 on 30/8/23.
 */
public class InstanceCloudSyncConflictData extends BaseData {

    public final UUID uuid;
    public final String code;
    public final String message;

    public InstanceCloudSyncConflictData(UUID uuid, String code, String message) {
        this.uuid = uuid;
        this.code = code;
        this.message = message;
    }
}
