package dev.ftb.app.api.data.other;

import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.storage.settings.SettingsData;

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
