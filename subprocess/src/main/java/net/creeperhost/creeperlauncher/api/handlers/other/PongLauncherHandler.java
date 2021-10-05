package net.creeperhost.creeperlauncher.api.handlers.other;

import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.api.data.other.PongLauncherData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;

public class PongLauncherHandler implements IMessageHandler<PongLauncherData> {
    @Override
    public void handle(PongLauncherData data) {
        CreeperLauncher.missedPings = 0;
    }
}
