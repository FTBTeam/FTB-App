package net.creeperhost.creeperlauncher.api.handlers.other;

import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.other.SettingsInfoData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;

import java.util.HashMap;

public class SettingsInfoHandler implements IMessageHandler<SettingsInfoData>
{
    @Override
    public void handle(SettingsInfoData data)
    {
        HashMap<String, String> settingsInfo = Settings.settings;
        Settings.webSocketAPI.sendMessage(new SettingsInfoData.Reply(data, settingsInfo));
    }
}
