package net.creeperhost.creeperlauncher.api.handlers.friends;

import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.friends.GetFriendsData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.minetogether.lib.chat.KnownUsers;
import net.creeperhost.minetogether.lib.chat.data.Profile;

import java.util.List;
import java.util.stream.Collectors;

public class GetFriendsHandler implements IMessageHandler<GetFriendsData> {
    @Override
    public void handle(GetFriendsData data) {
        List<Profile> online = KnownUsers.getFriends().stream().filter(Profile::isOnline).collect(Collectors.toList());
        List<Profile> offline = KnownUsers.getFriends().stream().filter((p) -> !p.isOnline()).collect(Collectors.toList());
        Settings.webSocketAPI.sendMessage(new GetFriendsData.Reply(online, offline));
    }
}
