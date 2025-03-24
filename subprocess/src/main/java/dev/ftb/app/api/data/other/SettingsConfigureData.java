package dev.ftb.app.api.data.other;

import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.storage.settings.SettingsData;
import org.jetbrains.annotations.Nullable;

public class SettingsConfigureData extends BaseData {
    public SettingsData settings;

    public static class Reply extends BaseData {
        String status;
        @Nullable
        SettingsData settings;

        public Reply(SettingsConfigureData data, String status, @Nullable SettingsData settingsData) {
            type = "saveSettingsReply";
            this.requestId = data.requestId;
            this.status = status;
            this.settings = settingsData;
        }
    }
}
