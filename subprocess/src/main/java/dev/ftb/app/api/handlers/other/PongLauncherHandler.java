package dev.ftb.app.api.handlers.other;

import dev.ftb.app.api.data.other.PongLauncherData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;

public class PongLauncherHandler implements IMessageHandler<PongLauncherData> {
    @Override
    public void handle(MessageContext context, PongLauncherData data) {}
}
