package dev.ftb.app.api.handlers.instances;

import dev.ftb.app.AppMain;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.instances.CancelInstallInstanceData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.task.InstallationOperation;

public class CancelInstallInstanceHandler implements IMessageHandler<CancelInstallInstanceData> {

    @Override
    public void handle(CancelInstallInstanceData data) {
        InstallationOperation op = AppMain.LONG_TASK_MANAGER.getRunningOperation(InstallationOperation.class);
        if (op != null) {
            op.cancel();
            WebSocketHandler.sendMessage(new CancelInstallInstanceData.Reply(data, "success", "Triggered cancellation.", op.getInstance().getUuid().toString()));
        }
    }
}
