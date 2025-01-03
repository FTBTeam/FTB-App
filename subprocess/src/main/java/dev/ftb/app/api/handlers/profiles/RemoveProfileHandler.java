package dev.ftb.app.api.handlers.profiles;

import dev.ftb.app.accounts.AccountManager;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.handlers.IMessageHandler;


import java.util.UUID;

public class RemoveProfileHandler implements IMessageHandler<RemoveProfileHandler.Data> {
    @Override
    public void handle(Data data) {
        WebSocketHandler.sendMessage(new Reply(data, AccountManager.get().removeProfile(data.uuid)));
    }

    public static class Data extends BaseData {
        UUID uuid;
    }

    private static class Reply extends Data {
        boolean success;

        public Reply(Data data, boolean success) {
            this.requestId = data.requestId;
            this.type = data.type + "Reply";
            this.success = success;
        }
    }
}
