package dev.ftb.app.api.handlers.instances;

import dev.ftb.app.api.data.instances.CancelInstallInstanceData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;
import dev.ftb.app.task.InstallationOperation;

public class CancelInstallInstanceHandler implements IMessageHandler<CancelInstallInstanceData> {

    @Override
    public void handle(MessageContext context, CancelInstallInstanceData data) {
        InstallationOperation op = context.services().threads().longRunningTaskManager().getRunningOperation(InstallationOperation.class);
        if (op != null) {
            op.cancel();
            context.reply(new CancelInstallInstanceData.Reply(data, "success", "Triggered cancellation.", op.getInstance().getUuid().toString()));
            return;
        }

        context.reply(new CancelInstallInstanceData.Reply(data, "error", "No installation operation is currently running.", null));
    }
}
