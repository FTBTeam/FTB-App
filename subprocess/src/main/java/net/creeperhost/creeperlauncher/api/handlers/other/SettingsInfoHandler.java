package net.creeperhost.creeperlauncher.api.handlers.other;

import net.creeperhost.creeperlauncher.api.WebSocketHandler;
import net.creeperhost.creeperlauncher.api.data.other.SettingsInfoData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.storage.settings.Settings;

public class SettingsInfoHandler implements IMessageHandler<SettingsInfoData>
{
    @Override
    public void handle(SettingsInfoData data) {
        WebSocketHandler.sendMessage(new SettingsInfoData.Reply(data, Settings.getSettings()));
    }
}
