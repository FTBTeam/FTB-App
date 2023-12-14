package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.api.WebSocketHandler;
import net.creeperhost.creeperlauncher.api.data.instances.InstanceVersionInfoData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.pack.Instance;

/**
 * Created by covers1624 on 2/6/22.
 */
public class InstanceVersionInfoHandler implements IMessageHandler<InstanceVersionInfoData> {

    @Override
    public void handle(InstanceVersionInfoData data) {
        Instance instance = Instances.getInstance(data.uuid);
        if (instance == null) {
            WebSocketHandler.sendMessage(new InstanceVersionInfoData.Reply(data, "error", "Instance does not exist", null));
            return;
        }
        WebSocketHandler.sendMessage(new InstanceVersionInfoData.Reply(data, "success", "", instance.versionManifest));
    }
}
