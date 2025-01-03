package dev.ftb.app.api.data.instances;

import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.data.InstanceJson;

import java.util.List;
import java.util.UUID;

public class CloudSavesReloadedData extends BaseData {

    public final List<InstanceJson> changedInstances;
    public final List<UUID> removedInstances;

    public CloudSavesReloadedData(List<InstanceJson> changedInstances, List<UUID> removedInstances) {
        type = "cloudInstancesReloaded";
        this.changedInstances = changedInstances;
        this.removedInstances = removedInstances;
    }
}
