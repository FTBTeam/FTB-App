package dev.ftb.app.api.handlers.profiles;

import dev.ftb.app.accounts.AccountManager;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.data.PrivateBaseData;
import dev.ftb.app.api.handlers.IMessageHandler;

import java.util.UUID;

public class SetActiveProfileHandler implements IMessageHandler<SetActiveProfileHandler.Data> {
    @Override
    public void handle(SetActiveProfileHandler.Data data) {
        AccountManager.get().setActiveProfile(data.uuid);
        WebSocketHandler.sendMessage(new Reply(
                data,
                true
        ));
    }

    public static class Data extends BaseData {
        public UUID uuid;
    }

    private static class Reply extends PrivateBaseData {
        boolean success;

        public Reply(BaseData data, boolean success) {
            this.requestId = data.requestId;
            this.type = data.type + "Reply";

            this.success = success;
        }
    }
}
