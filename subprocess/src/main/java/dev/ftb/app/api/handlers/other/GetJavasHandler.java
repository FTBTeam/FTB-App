package dev.ftb.app.api.handlers.other;

import dev.ftb.app.AppMain;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.other.GetJavasData;
import dev.ftb.app.api.handlers.IMessageHandler;

public class GetJavasHandler implements IMessageHandler<GetJavasData> {

    @Override
    public void handle(GetJavasData data) {
        WebSocketHandler.sendMessage(new GetJavasData.Reply(data, AppMain.javaVersions));
    }
}
