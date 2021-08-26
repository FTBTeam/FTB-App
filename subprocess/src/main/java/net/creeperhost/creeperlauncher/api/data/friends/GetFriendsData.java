package net.creeperhost.creeperlauncher.api.data.friends;

import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.minetogether.lib.chat.data.Profile;

import java.util.List;

public class GetFriendsData extends BaseData {

    public static class Reply extends BaseData {
        List<Profile> online;
        List<Profile> offline;
        Reply(List<Profile> online, List<Profile> offline) {
            this.type = "ircFriendsReply";
            this.online = online;
            this.offline = offline;
        }
    }
}
