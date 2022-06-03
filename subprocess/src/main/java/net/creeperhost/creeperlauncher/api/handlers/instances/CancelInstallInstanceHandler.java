package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.instances.CancelInstallInstanceData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.task.InstallationOperation;

public class CancelInstallInstanceHandler implements IMessageHandler<CancelInstallInstanceData> {

    @Override
    public void handle(CancelInstallInstanceData data) {
        InstallationOperation op = CreeperLauncher.LONG_TASK_MANAGER.getRunningOperation(InstallationOperation.class);
        if (op != null) {
            op.cancel();
            Settings.webSocketAPI.sendMessage(new CancelInstallInstanceData.Reply(data, "success", "Triggered cancellation.", op.getInstance().getUuid().toString()));
        }
    }
}
