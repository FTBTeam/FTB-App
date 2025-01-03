package dev.ftb.app.api.handlers.other;

import dev.ftb.app.AppMain;
import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.handlers.IMessageHandler;

public class OpenDebugToolsHandler implements IMessageHandler<BaseData> {

    @Override
    public void handle(BaseData data) {
        AppMain.openDebugTools();
    }
}
