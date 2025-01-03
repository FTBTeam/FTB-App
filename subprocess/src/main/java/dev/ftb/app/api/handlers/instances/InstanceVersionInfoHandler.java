package dev.ftb.app.api.handlers.instances;

import dev.ftb.app.Instances;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.instances.InstanceVersionInfoData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.pack.Instance;

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
