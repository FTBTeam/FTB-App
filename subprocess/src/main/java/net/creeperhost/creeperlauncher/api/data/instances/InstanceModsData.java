package net.creeperhost.creeperlauncher.api.data.instances;

import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.data.mod.CurseMetadata;
import net.creeperhost.creeperlauncher.data.mod.ModInfo;

import java.util.List;
import java.util.UUID;

public class InstanceModsData extends BaseData {

    public UUID uuid;
    public boolean _private = false;

    public static class Reply extends InstanceModsData {

        public List<ModInfo> files;

        public Reply(InstanceModsData data, List<ModInfo> files) {
            type = "instanceModsReply";
            uuid = data.uuid;
            requestId = data.requestId;
            this.files = files;
        }
    }

    public static class UpdateAvailable extends InstanceModsData {

        public final ModInfo file;
        public final CurseMetadata update;

        public UpdateAvailable(InstanceModsData data, ModInfo file, CurseMetadata update) {
            type = "instanceModUpdate";
            this.requestId = data.requestId;
            this.file = file;
            this.update = update;
        }
    }

    public static class UpdateCheckingFinished extends InstanceModsData {

        public UpdateCheckingFinished(InstanceModsData data) {
            type = "instanceModUpdateCheckingFinished";
            uuid = data.uuid;
            requestId = data.requestId;
        }
    }
}
