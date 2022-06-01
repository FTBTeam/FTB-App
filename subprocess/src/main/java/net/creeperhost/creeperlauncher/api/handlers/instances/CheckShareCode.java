package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.instances.CheckShareCodeData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest;

import java.io.IOException;

public class CheckShareCode implements IMessageHandler<CheckShareCodeData> {
    
    @Override
    public void handle(CheckShareCodeData data) {
        boolean success;
        try {
            ModpackVersionManifest manifest = ModpackVersionManifest.queryManifest(Constants.TRANSFER_HOST + data.shareCode + "/version.json");
            success = manifest != null;
        } catch (IOException e) {
            success = false;
        }
        Settings.webSocketAPI.sendMessage(new CheckShareCodeData.Reply(data, success));
    }
}
