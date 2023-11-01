package net.creeperhost.creeperlauncher.api.data.instances;

import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.data.InstanceJson;
import org.jetbrains.annotations.Nullable;

public class InstallInstanceData extends BaseData {

    public String uuid;
    public long id;
    public long version;
    public boolean _private = false;
    public byte packType = 0;
    public @Nullable String mcVersion;
    public String shareCode;
    @Nullable
    public String importFrom;
    @Nullable public String name;
    @Nullable public String artPath;
    @Nullable public String category;

    public static class Reply extends BaseData {

        public final String status;
        public final String message;
        @Nullable
        public final InstanceJson instanceData;

        public Reply(InstallInstanceData data, String status, String message) {
            this(data, status, message, null);
        }

        public Reply(InstallInstanceData data, String status, String message, InstanceJson props) {
            type = "installInstanceDataReply";
            requestId = data.requestId;
            this.status = status;
            this.message = message;
            this.instanceData = props;
        }
    }
}
