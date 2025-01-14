package dev.ftb.app.migration.migrations;

import dev.ftb.app.Constants;
import dev.ftb.app.migration.Migration;
import dev.ftb.app.storage.settings.Settings;
import dev.ftb.app.storage.settings.SettingsData;

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
