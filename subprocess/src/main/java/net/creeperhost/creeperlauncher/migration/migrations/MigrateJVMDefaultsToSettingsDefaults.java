package net.creeperhost.creeperlauncher.migration.migrations;

import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.migration.Migration;
import net.creeperhost.creeperlauncher.storage.settings.Settings;
import net.creeperhost.creeperlauncher.storage.settings.SettingsData;

public class MigrateJVMDefaultsToSettingsDefaults implements Migration {
    @Override
    public String id() {
        return "migrate_jvm_defaults_to_settings_defaults";
    }

    @Override
    public boolean migrate() {
        SettingsData settings = Settings.getSettings();
        
        if (settings.instanceDefaults().javaArgs().isEmpty()) {
            settings.instanceDefaults().setJavaArgs(String.join(" ", Constants.MOJANG_DEFAULT_ARGS));
            Settings.saveSettings();
        }
        
        return true;
    }
}
