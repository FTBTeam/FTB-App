package dev.ftb.app.api.handlers.other;

import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.other.SettingsConfigureData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.storage.CredentialStorage;
import dev.ftb.app.storage.settings.Settings;
import dev.ftb.app.util.SettingsChangeUtil;
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

            if (!data.settings.proxy().password().isEmpty()) {
                CredentialStorage.getInstance().set("proxyPassword", data.settings.proxy().password());
                data.settings.proxy().setPassword("");
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
