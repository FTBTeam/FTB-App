package dev.ftb.app.api.handlers.other;

import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.other.SettingsInfoData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.storage.settings.Settings;

public class SettingsInfoHandler implements IMessageHandler<SettingsInfoData>
{
    @Override
    public void handle(SettingsInfoData data) {
        WebSocketHandler.sendMessage(new SettingsInfoData.Reply(data, Settings.getSettings()));
    }
}
