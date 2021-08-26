package net.creeperhost.creeperlauncher.api.data.friends;

import net.creeperhost.minetogether.lib.chat.data.Profile;

public class OnlineFriendData extends FriendData {
    public OnlineFriendData(Profile profile) {
        super(profile);
        this.type = "ircOnlineFriend";
    }
}
