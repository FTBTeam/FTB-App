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
        try {
            ModpackVersionManifest.queryManifest(Constants.TRANSFER_HOST + data.shareCode + "/version.json");
            Settings.webSocketAPI.sendMessage(new CheckShareCodeData.Reply(data, true));
        } catch (IOException e) {
            Settings.webSocketAPI.sendMessage(new CheckShareCodeData.Reply(data, false));
        }
    }
}
