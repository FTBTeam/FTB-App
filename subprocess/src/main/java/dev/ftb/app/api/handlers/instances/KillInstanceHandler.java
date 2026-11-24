package dev.ftb.app.api.handlers.instances;

import dev.ftb.app.api.data.instances.KillInstanceData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;
import dev.ftb.app.pack.Instance;
import dev.ftb.app.pack.InstanceLauncher;

public class KillInstanceHandler implements IMessageHandler<KillInstanceData> {

    @Override
    public void handle(MessageContext context, KillInstanceData data) {
        Instance instance = context.services().instances().getInstance(data.uuid);
        if (instance == null) {
            context.reply(new KillInstanceData.Reply(data, "error", "Instance does not exist"));
            return;
        }

        InstanceLauncher launcher = instance.getLauncher();

        if (!launcher.isRunning()) {
            context.reply(new KillInstanceData.Reply(data, "error", "Instance is not running"));
            return;
        }

        instance.forceStop();
        context.reply(new KillInstanceData.Reply(data, "success", "Instance force stopped"));
    }
}
