package net.creeperhost.creeperlauncher.api.data.friends;

import net.creeperhost.creeperlauncher.api.data.BaseData;

public class AcceptedFriendData extends BaseData {
    private final String friendName;

    public AcceptedFriendData(String friendName) {
        this.type = "ircAcceptedFriend";
        this.friendName = friendName;
    }
}
