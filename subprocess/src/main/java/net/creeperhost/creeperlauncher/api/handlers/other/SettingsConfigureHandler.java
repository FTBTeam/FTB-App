package net.creeperhost.creeperlauncher.api.handlers.other;

import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.other.SettingsConfigureData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.util.SettingsChangeUtil;

import java.util.Map;

public class SettingsConfigureHandler implements IMessageHandler<SettingsConfigureData>
{
    @Override
    public void handle(SettingsConfigureData data)
    {
        boolean anyChanged = false;
        for (Map.Entry<String, String> setting : data.settingsInfo.entrySet())
        {
            try {
                boolean changed = false;
                if (!Settings.settings.containsKey(setting.getKey()) || !Settings.settings.get(setting.getKey()).equals(setting.getValue()))
                    changed = true;
                if (changed) {
                    if (SettingsChangeUtil.settingsChanged(setting.getKey(), setting.getValue())) {
                        Settings.settings.remove(setting.getKey());
                        Settings.settings.put(setting.getKey(), setting.getValue());
                        anyChanged = true;
                    }
                }
            } catch (Exception e) {
            }
        }
        if (anyChanged)
        {
            Settings.saveSettings();
        }
        Settings.webSocketAPI.sendMessage(new SettingsConfigureData.Reply(data, "success"));
    }
}
