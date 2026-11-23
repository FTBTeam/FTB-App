package dev.ftb.app.api.handlers.profiles;

import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.data.PrivateBaseData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;
import dev.ftb.app.structs.accounts.AccountProvider;
import dev.ftb.app.util.MiscUtils;

import java.util.UUID;

public class SetActiveProfileHandler implements IMessageHandler<SetActiveProfileHandler.Data> {
    @Override
    public void handle(MessageContext context, Data data) {
        var accounts = context.services().accounts();

        boolean success = switch (data.provider) {
            case MICROSOFT -> {
                UUID uuid = MiscUtils.tryParseUuid(data.id);
                yield uuid != null && accounts.minecraft().setActive(uuid);
            }
            case FTB -> accounts.ftb().setActive(data.id);
        };

        context.reply(new Reply(data, success));
    }

    public static class Data extends BaseData {
        public AccountProvider provider;
        /// We support either a UUID or a normal ID (sub) from keycloak
        public String id;
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
