package net.creeperhost.creeperlauncher.api.handlers.other;

import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.api.data.other.YeetLauncherData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;

public class YeetLauncherHandler implements IMessageHandler<YeetLauncherData> {
    @Override
    public void handle(YeetLauncherData data) {
        CreeperLauncher.exit();
    }
}
