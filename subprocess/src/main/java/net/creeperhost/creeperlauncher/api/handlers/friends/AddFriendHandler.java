package net.creeperhost.creeperlauncher.api.handlers.friends;

import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.friends.AddFriendData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.minetogether.lib.chat.ChatCallbacks;
import net.creeperhost.minetogether.lib.chat.MineTogetherChat;
import net.creeperhost.minetogether.lib.serverlists.FriendStatusResponse;

public class AddFriendHandler implements IMessageHandler<AddFriendData> {
    @Override
    public void handle(AddFriendData data) {
        FriendStatusResponse friendStatusResponse = ChatCallbacks.addFriend(data.target, data.target, MineTogetherChat.INSTANCE.hash);
        Settings.webSocketAPI.sendMessage(new AddFriendData.Reply(data.requestId, friendStatusResponse.isSuccess(), friendStatusResponse.message, friendStatusResponse.hash));
    }
}
