package dev.ftb.app.api.handlers.other;

import dev.ftb.app.api.data.other.SettingsConfigureData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;
import dev.ftb.app.storage.settings.Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SettingsConfigureHandler implements IMessageHandler<SettingsConfigureData> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(MessageContext context, SettingsConfigureData data) {
        // Is this a waste to parse the obj to json just to compare it to the data provided?
        Settings settings = context.services().settings();
        
        try {
            var originalSettings = settings.settings().toString();
            var newSettings = data.settings.toString();

            if (originalSettings.equals(newSettings)) {
                context.reply(new SettingsConfigureData.Reply(data, "success", data.settings));
                return;
            }

            if (!data.settings.proxy().password().isEmpty()) {
                context.services().storage().vault().put("proxyPassword", data.settings.proxy().password());
                data.settings.proxy().setPassword(""); // Ensure the legacy field isn't populated.
            }

            settings.store().set(data.settings);            
            context.reply(new SettingsConfigureData.Reply(data, "success", data.settings));
        } catch (Exception e) {
            LOGGER.error("Failed to handle settings change event.", e);
            context.reply(new SettingsConfigureData.Reply(data, "failed", null));
        }
    }
}
