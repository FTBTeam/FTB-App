package net.creeperhost.creeperlauncher.api.data.friends;

import net.creeperhost.creeperlauncher.api.data.BaseData;

public class AcceptedFriendData extends BaseData {
    private final String friendName;
    private final Object friendData;

    public AcceptedFriendData(String friendName, String data) {
        this.type = "ircAcceptedFriend";
        this.friendName = friendName;
        this.friendData = data;
    }
}
