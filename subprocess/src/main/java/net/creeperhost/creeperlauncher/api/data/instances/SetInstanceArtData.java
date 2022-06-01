package net.creeperhost.creeperlauncher.api.data.instances;

import net.creeperhost.creeperlauncher.api.data.BaseData;

import java.util.UUID;

/**
 * Created by covers1624 on 1/6/22.
 */
public class SetInstanceArtData extends BaseData {

    public UUID uuid;
    public String artPath;

    public static class Reply extends BaseData {

        public String status;
        public String message;

        public Reply(SetInstanceArtData data, String status, String message) {
            requestId = data.requestId;
            type = "setInstanceArtReply";
            this.status = status;
            this.message = message;
        }
    }
}
