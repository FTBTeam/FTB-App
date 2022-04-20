package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.instances.InstallSharedInstanceData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by covers1624 on 20/4/22.
 */
public class InstallSharedInstanceHandler implements IMessageHandler<InstallSharedInstanceData> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(InstallSharedInstanceData data) {

        if (CreeperLauncher.isInstalling) {
            assert CreeperLauncher.currentInstall != null;
            Settings.webSocketAPI.sendMessage(new InstallSharedInstanceData.Reply(data, "error", "Install in progress.", CreeperLauncher.currentInstall.getInstance().getUuid()));
            return;
        }
        CreeperLauncher.isInstalling = true;
        Settings.webSocketAPI.sendMessage(new InstallSharedInstanceData.Reply(data, "init", "Install started.", null));
        try {


        } catch (Throwable ex) {

        }

    }
}
