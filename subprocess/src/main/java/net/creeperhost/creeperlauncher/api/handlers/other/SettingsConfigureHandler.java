package net.creeperhost.creeperlauncher.api.handlers.other;

import com.google.common.collect.ImmutableMap;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.other.SettingsConfigureData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.util.SettingsChangeUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public class SettingsConfigureHandler implements IMessageHandler<SettingsConfigureData> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(SettingsConfigureData data) {
        boolean anyChanged = false;
        Map<String, String> oldSettings = ImmutableMap.copyOf(Settings.settings);
        for (Map.Entry<String, String> setting : data.settingsInfo.entrySet()) {
            try {
                if (!Settings.settings.containsKey(setting.getKey()) || !Settings.settings.get(setting.getKey()).equals(setting.getValue())) {
                    if (SettingsChangeUtil.handleSettingChange(setting.getKey(), setting.getValue())) {
                        Settings.settings.remove(setting.getKey());
                        Settings.settings.put(setting.getKey(), setting.getValue());
                        anyChanged = true;
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Failed to handle settings change event.", e);
            }
        }
        if (anyChanged) {
            SettingsChangeUtil.onSettingsChanged(oldSettings);
            Settings.saveSettings();
        }
        Settings.webSocketAPI.sendMessage(new SettingsConfigureData.Reply(data, "success"));
    }
}
