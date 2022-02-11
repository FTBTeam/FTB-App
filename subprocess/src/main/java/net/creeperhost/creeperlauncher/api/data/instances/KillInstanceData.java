package net.creeperhost.creeperlauncher.api.data.instances;

import net.creeperhost.creeperlauncher.api.data.BaseData;

import java.util.UUID;

/**
 * Created by covers1624 on 9/2/22.
 */
public class KillInstanceData extends BaseData {

    public UUID uuid;

    public static class Reply extends BaseData {

        public final String status;
        public final String message;

        public Reply(KillInstanceData data, String status, String message) {
            requestId = data.requestId;
            type = "instance.kill.reply";
            this.status = status;
            this.message = message;
        }
    }
}
