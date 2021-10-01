package net.creeperhost.creeperlauncher.api.data.friends;

import net.creeperhost.creeperlauncher.api.data.BaseData;

public class RequestFriendData extends BaseData {
    private final String friendName;
    private final Object friendData;

    public RequestFriendData(String friendName, String data) {
        this.type = "ircRequestedFriend";
        this.friendName = friendName;
        this.friendData = data;
    }
}
