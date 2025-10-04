package dev.ftb.app.api.handlers.other;

import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.other.ConfigureWindowsGpuData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.os.OS;
import dev.ftb.app.util.WindowsGpuPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for configuring Windows GPU preferences for Java runtimes.
 */
public class ConfigureWindowsGpuHandler implements IMessageHandler<ConfigureWindowsGpuData> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigureWindowsGpuHandler.class);
    
    @Override
    public void handle(ConfigureWindowsGpuData data) {
        if (OS.CURRENT != OS.WIN) {
            LOGGER.info("Not on Windows, skipping GPU preference configuration");
            WebSocketHandler.sendMessage(new ConfigureWindowsGpuData.Reply(data, false, "Not on Windows"));
            return;
        }
        
        LOGGER.info("Configuring Windows GPU preferences for all Java runtimes");
        boolean success = WindowsGpuPreferences.configureAllJavaRuntimes();
        
        if (success) {
            LOGGER.info("Successfully configured GPU preferences");
            WebSocketHandler.sendMessage(new ConfigureWindowsGpuData.Reply(data, true, "Successfully configured GPU preferences"));
        } else {
            LOGGER.error("Failed to configure GPU preferences");
            WebSocketHandler.sendMessage(new ConfigureWindowsGpuData.Reply(data, false, "Failed to configure some GPU preferences"));
        }
    }
}
