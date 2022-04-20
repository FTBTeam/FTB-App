package net.creeperhost.creeperlauncher.api.data.instances;

import net.creeperhost.creeperlauncher.api.data.BaseData;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Created by covers1624 on 20/4/22.
 */
public class InstallSharedInstanceData extends BaseData {

    public String shareCode;

    public static class Reply extends BaseData {

        public final String status;
        @Nullable
        public final String message;
        @Nullable
        public final UUID uuid;

        public Reply(InstallSharedInstanceData data, String status, @Nullable String message, @Nullable UUID uuid) {
            type = "installSharedInstanceReply";
            requestId = data.requestId;
            this.status = status;
            this.message = message;
            this.uuid = uuid;
        }
    }
}
