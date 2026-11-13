package dev.ftb.app.api.handlers.profiles;

import dev.ftb.app.services.accounts.McAccounts;
import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;

import java.util.UUID;

public class RemoveProfileHandler implements IMessageHandler<RemoveProfileHandler.Data> {
    @Override
    public void handle(MessageContext context, Data data) {
        McAccounts accounts = context.services().accounts().minecraft();
        accounts.remove(data.uuid);

        // Success means it's gone, including when it was never there
        context.reply(new Reply(data, accounts.get(data.uuid) == null));
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
