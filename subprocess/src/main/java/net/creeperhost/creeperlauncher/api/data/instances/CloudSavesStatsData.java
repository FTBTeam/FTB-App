package net.creeperhost.creeperlauncher.api.data.instances;

import net.creeperhost.creeperlauncher.api.data.BaseData;

/**
 * Created by covers1624 on 19/10/23.
 */
public class CloudSavesStatsData extends BaseData {

    public final long bucketSize;

    public CloudSavesStatsData(long bucketSize) {
        type = "cloudSavesStats";
        this.bucketSize = bucketSize;
    }
}
