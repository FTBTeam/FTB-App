package net.creeperhost.creeperlauncher.api.handlers.friends;

import com.google.gson.reflect.TypeToken;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.friends.BlockFriendData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.util.GsonUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class BlockFriendHandler implements IMessageHandler<BlockFriendData> {
    @Override
    public void handle(BlockFriendData data) {
        List<String> blockedUsers;
        if(Settings.settings.containsKey("blockedUsers")){
            String blockedString = Settings.settings.get("blockedUsers");
            Type listOfString = new TypeToken<ArrayList<String>>() {}.getType();
            blockedUsers = GsonUtils.GSON.fromJson(blockedString, listOfString);
        } else {
            blockedUsers = new ArrayList<>();
        }
        if(blockedUsers.contains(data.hash)){
            return;
        }
        blockedUsers.add(data.hash);
        Settings.settings.put("blockedUsers", GsonUtils.GSON.toJson(blockedUsers));
    }
}
