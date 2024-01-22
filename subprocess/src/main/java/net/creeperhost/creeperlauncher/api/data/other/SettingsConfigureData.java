package net.creeperhost.creeperlauncher.api.data.other;

import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.storage.settings.SettingsData;

public class SettingsConfigureData extends BaseData {
    public SettingsData settings;

    public static class Reply extends BaseData {
        String status;

        public Reply(SettingsConfigureData data, String status) {
            type = "saveSettingsReply";
            this.requestId = data.requestId;
            this.status = status;
        }
    }
}
