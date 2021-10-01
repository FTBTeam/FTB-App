package net.creeperhost.creeperlauncher.api.data.friends;

import net.creeperhost.creeperlauncher.api.data.BaseData;

public class AddFriendData extends BaseData {
    public String target;

    public AddFriendData() {
    }

    public static class Reply extends BaseData {
        private boolean status;
        private String message;
        private String hash;
        public Reply(int requestId, boolean status, String message, String hash) {
            this.type = "addFriendReply";
            this.requestId = requestId;
            this.hash  = hash;
            this.status = status;
            this.message = message;
        }

    }

}
