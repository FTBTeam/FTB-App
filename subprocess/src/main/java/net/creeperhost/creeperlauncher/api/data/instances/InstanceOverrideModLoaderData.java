package net.creeperhost.creeperlauncher.api.data.instances;

import net.creeperhost.creeperlauncher.api.data.BaseData;

import java.util.UUID;

/**
 * Created by covers1624 on 6/9/23.
 */
public class InstanceOverrideModLoaderData extends BaseData {

    public UUID uuid;
    public long modLoaderId;
    public long modLoaderVersion;

    public static class Reply extends BaseData {

        public final String status;
        public final String message;

        public Reply(InstanceOverrideModLoaderData req, String status, String message) {
            type = "instanceOverrideModLoaderReply";
            requestId = req.requestId;
            this.status = status;
            this.message = message;
        }
    }
}
