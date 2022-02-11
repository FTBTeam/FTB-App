package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.instances.CancelInstallInstanceData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;

public class CancelInstallInstanceHandler implements IMessageHandler<CancelInstallInstanceData> {

    @Override
    public void handle(CancelInstallInstanceData data) {
        if (CreeperLauncher.isInstalling) {
            assert CreeperLauncher.currentInstall != null;
            assert CreeperLauncher.currentInstallFuture != null;
            CreeperLauncher.currentInstall.cancel(CreeperLauncher.currentInstallFuture);
            Settings.webSocketAPI.sendMessage(new CancelInstallInstanceData.Reply(data, "success", "Cancelled Install", CreeperLauncher.currentInstall.getInstance().getUuid().toString()));
        }
    }
}
