package dev.ftb.app.api.data.instances;

import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.handlers.instances.InstalledInstancesHandler;
import dev.ftb.app.data.InstanceJson;
import dev.ftb.app.pack.Instance;
import org.jetbrains.annotations.Nullable;

public class InstallInstanceData extends BaseData {

    public String uuid;
    public long id;
    public long version;
    public boolean _private = false;
    public byte packType = 0;
    public @Nullable String mcVersion;
    @Nullable
    public String importFrom;
    @Nullable
    public String name;
    @Nullable
    public String artPath;
    @Nullable
    public String category;

    // QOL stuff for own instances
    public boolean ourOwn = false; // Marker for when it's a custom instance creation
    public int ram = -1; // Default
    @Nullable
    public Boolean fullscreen = false; // default
    public int screenWidth = -1; // default
    public int screenHeight = -1; // default

    public static class Reply extends BaseData {

        public final String status;
        public final String message;
        @Nullable
        public final InstanceJson instanceData;

        public Reply(InstallInstanceData data, String status, String message) {
            this(data, status, message, null);
        }

        public Reply(InstallInstanceData data, String status, String message, @Nullable Instance instance) {
            type = "installInstanceDataReply";
            requestId = data.requestId;
            this.status = status;
            this.message = message;

            if (instance == null) {
                instanceData = null;
            } else if (instance.props.installComplete) {
                // Send a sugared instance json to the frontend upon completion
                this.instanceData = new InstalledInstancesHandler.SugaredInstanceJson(instance);
            } else {
                this.instanceData = instance.props;
            }
        }
    }
}
