package dev.ftb.app.api.data.other;

import dev.ftb.app.api.data.BaseData;

public class ConfigureWindowsGpuData extends BaseData {

    public static class Reply extends BaseData {
        public final boolean success;
        public final String message;

        public Reply(ConfigureWindowsGpuData data, boolean success, String message) {
            this.requestId = data.requestId;
            this.success = success;
            this.message = message;
        }
    }
}
