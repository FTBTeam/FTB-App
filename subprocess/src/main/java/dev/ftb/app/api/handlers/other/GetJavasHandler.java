package dev.ftb.app.api.handlers.other;

import dev.ftb.app.AppMain;
import dev.ftb.app.api.data.other.GetJavasData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;

public class GetJavasHandler implements IMessageHandler<GetJavasData> {
    @Override
    public void handle(MessageContext context, GetJavasData data) {
        context.reply(new GetJavasData.Reply(data, AppMain.javaVersions));
    }
}
