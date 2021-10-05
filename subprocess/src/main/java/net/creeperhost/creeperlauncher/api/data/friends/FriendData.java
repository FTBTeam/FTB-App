package net.creeperhost.creeperlauncher.api.data.friends;

import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.minetogether.lib.chat.data.Profile;

public abstract class FriendData extends BaseData {
    Profile profile;
    public FriendData(Profile profile){
        this.profile = profile;
        type = "ircFriendProfile";
    }
}
