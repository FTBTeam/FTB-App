package dev.ftb.app.api.handlers.other;

import dev.ftb.app.api.data.other.SettingsInfoData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;

public class SettingsInfoHandler implements IMessageHandler<SettingsInfoData> {
    @Override
    public void handle(MessageContext context, SettingsInfoData data) {
        context.reply(new SettingsInfoData.Reply(data, context.services().settings().get()));
    }
}
