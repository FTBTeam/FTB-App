package dev.ftb.app.api.data.instances;

import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.data.modpack.ModpackVersionManifest;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class InstanceVersionInfoData extends BaseData {

    public UUID uuid;

    public static class Reply extends BaseData {

        public String status;
        public String message;
        @Nullable
        public ModpackVersionManifest versionManifest;

        public Reply(InstanceVersionInfoData data, String status, String message, @Nullable ModpackVersionManifest versionManifest) {
            this.type = "instanceVersionInfo";
            this.requestId = data.requestId;
            this.status = status;
            this.message = message;
            this.versionManifest = versionManifest;
        }
    }
}
