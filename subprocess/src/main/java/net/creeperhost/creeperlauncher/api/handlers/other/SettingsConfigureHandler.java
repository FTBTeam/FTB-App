package net.creeperhost.creeperlauncher.api.handlers.other;

import net.creeperhost.creeperlauncher.api.WebSocketHandler;
import net.creeperhost.creeperlauncher.api.data.other.SettingsConfigureData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.storage.settings.Settings;
import net.creeperhost.creeperlauncher.util.SettingsChangeUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SettingsConfigureHandler implements IMessageHandler<SettingsConfigureData> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(SettingsConfigureData data) {
        // Is this a waste to parse the obj to json just to compare it to the data provided?
        try {
            var originalSettings = Settings.getSettings().toString();
            var newSettings = data.settings.toString();

            if (originalSettings.equals(newSettings)) {
                WebSocketHandler.sendMessage(new SettingsConfigureData.Reply(data, "success"));
                return;
            }

            // Actually handle the settings change
            Settings.updateSettings(data.settings);
            // Dispatch the event
            SettingsChangeUtil.onSettingsChanged(Settings.getSettings());
            
            WebSocketHandler.sendMessage(new SettingsConfigureData.Reply(data, "success"));
        } catch (Exception e) {
            LOGGER.error("Failed to handle settings change event.", e);
            WebSocketHandler.sendMessage(new SettingsConfigureData.Reply(data, "failed"));
        }
    }
}
