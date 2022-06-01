package net.creeperhost.creeperlauncher.api.handlers.instances;

import com.google.gson.JsonSyntaxException;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.instances.CheckShareCodeData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest;
import net.creeperhost.creeperlauncher.data.modpack.ShareManifest;

import java.io.IOException;

public class CheckShareCode implements IMessageHandler<CheckShareCodeData> {
    
    @Override
    public void handle(CheckShareCodeData data) {
        boolean success;
        try {
            ShareManifest manifest = ShareManifest.queryManifest(Constants.TRANSFER_HOST + data.shareCode + "/manifest.json");
            success = manifest != null;
        } catch (JsonSyntaxException e) {
            success = false;
        }
        Settings.webSocketAPI.sendMessage(new CheckShareCodeData.Reply(data, success));
    }
}
