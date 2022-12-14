package net.creeperhost.creeperlauncher.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SettingsChangeUtil {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<String, ISettingsChangedHandler> changeHandlers = new HashMap<>();
    private static final List<SettingsChangedListener> changeListeners = new LinkedList<>();

    public static void registerChangeHandler(String key, ISettingsChangedHandler handler) {
        if (changeHandlers.containsKey(key)) {
            LOGGER.warn("New handler for {} when already exists, doing nothing", key);
            return;
        }
        changeHandlers.put(key, handler);
    }

    public static void addChangeListener(SettingsChangedListener listener) {
        changeListeners.add(listener);
    }

    public static boolean handleSettingChange(String key, String value) {
        ISettingsChangedHandler changedHandler = changeHandlers.get(key);
        if (changedHandler == null) return true;

        return changedHandler.handle(key, value);
    }

    public static void onSettingsChanged(Map<String, String> oldSettings) {
        for (SettingsChangedListener listener : changeListeners) {
            listener.onChange(oldSettings);
        }
    }

    public interface ISettingsChangedHandler {

        boolean handle(String key, String value);
    }

    public interface SettingsChangedListener {

        void onChange(Map<String, String> oldSettings);
    }
}
