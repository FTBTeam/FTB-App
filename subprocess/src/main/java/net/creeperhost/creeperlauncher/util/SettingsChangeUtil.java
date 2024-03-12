package net.creeperhost.creeperlauncher.util;

import net.creeperhost.creeperlauncher.storage.settings.SettingsData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;

public class SettingsChangeUtil {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final List<SettingsChangedListener> changeListeners = new LinkedList<>();
    
    public static void addChangeListener(SettingsChangedListener listener) {
        changeListeners.add(listener);
    }

    public static void onSettingsChanged(SettingsData settings) {
        for (SettingsChangedListener listener : changeListeners) {
            LOGGER.info("Dispatching settings change event to listener: " + listener.getClass().getName());
            listener.onChange(settings);
        }
    }

    public interface SettingsChangedListener {

        void onChange(SettingsData settings);
    }
}
