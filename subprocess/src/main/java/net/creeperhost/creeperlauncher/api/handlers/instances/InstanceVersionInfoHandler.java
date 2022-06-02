package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.instances.InstanceVersionInfoData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.pack.LocalInstance;

/**
 * Created by covers1624 on 2/6/22.
 */
public class InstanceVersionInfoHandler implements IMessageHandler<InstanceVersionInfoData> {

    @Override
    public void handle(InstanceVersionInfoData data) {
        LocalInstance instance = Instances.getInstance(data.uuid);
        if (instance == null) {
            Settings.webSocketAPI.sendMessage(new InstanceVersionInfoData.Reply(data, "error", "Instance does not exist", null));
            return;
        }
        Settings.webSocketAPI.sendMessage(new InstanceVersionInfoData.Reply(data, "success", "", instance.versionManifest));
    }
}
