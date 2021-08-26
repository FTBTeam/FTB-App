package net.creeperhost.creeperlauncher.api.handlers.other;

import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.other.GetJavasData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;

public class GetJavasHandler implements IMessageHandler<GetJavasData> {
    @Override
    public void handle(GetJavasData data) {
        Settings.webSocketAPI.sendMessage(new GetJavasData.Reply(data, CreeperLauncher.javaVersions));
    }
}
