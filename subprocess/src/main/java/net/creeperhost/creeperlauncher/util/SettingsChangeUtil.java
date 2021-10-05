package net.creeperhost.creeperlauncher.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

public class SettingsChangeUtil {
    private static final Logger LOGGER = LogManager.getLogger();

    private static HashMap<String, ISettingsChangedHandler> settingsChangedListeners = new HashMap<>();

    public static void registerListener(String key, ISettingsChangedHandler handler) {
        if (settingsChangedListeners.containsKey(key)) {
            LOGGER.warn("New handler for {} when already exists, doing nothing", key);
            return;
        }
        settingsChangedListeners.put(key, handler);
    }

    public static boolean settingsChanged(String key, String value) {
        if (settingsChangedListeners.containsKey(key)) {
            try {
                return settingsChangedListeners.get(key).handle(key, value);
            } catch (Exception e) {
            }
        }
        return true;
    }

    public interface ISettingsChangedHandler {
        boolean handle(String key, String value);
    }
}
