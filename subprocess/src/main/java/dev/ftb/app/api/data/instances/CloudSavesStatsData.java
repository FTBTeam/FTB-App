package dev.ftb.app.api.data.instances;

import dev.ftb.app.api.data.BaseData;

public class CloudSavesStatsData extends BaseData {

    public final long bucketSize;

    public CloudSavesStatsData(long bucketSize) {
        type = "cloudSavesStats";
        this.bucketSize = bucketSize;
    }
}
