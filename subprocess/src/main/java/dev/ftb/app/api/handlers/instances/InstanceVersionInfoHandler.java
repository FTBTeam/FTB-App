package dev.ftb.app.api.handlers.instances;

import dev.ftb.app.api.data.instances.InstanceVersionInfoData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;
import dev.ftb.app.pack.Instance;

public class InstanceVersionInfoHandler implements IMessageHandler<InstanceVersionInfoData> {

    @Override
    public void handle(MessageContext context, InstanceVersionInfoData data) {
        Instance instance = context.services().instances().getInstance(data.uuid);
        if (instance == null) {
            context.reply(new InstanceVersionInfoData.Reply(data, "error", "Instance does not exist", null));
            return;
        }
        context.reply(new InstanceVersionInfoData.Reply(data, "success", "", instance.versionManifest));
    }
}
