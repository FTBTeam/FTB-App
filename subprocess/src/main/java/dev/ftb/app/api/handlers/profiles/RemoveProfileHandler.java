package dev.ftb.app.api.handlers.profiles;

import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;
import dev.ftb.app.structs.accounts.AccountProvider;
import dev.ftb.app.util.MiscUtils;

import java.util.UUID;

public class RemoveProfileHandler implements IMessageHandler<RemoveProfileHandler.Data> {
    @Override
    public void handle(MessageContext context, Data data) {
        var accounts = context.services().accounts();

        // Success means it's gone, including when it was never there
        boolean gone = switch (data.provider) {
            case MICROSOFT -> {
                UUID uuid = MiscUtils.tryParseUuid(data.id);
                if (uuid == null) {
                    yield false;
                }

                accounts.minecraft().remove(uuid);
                yield accounts.minecraft().get(uuid) == null;
            }
            case FTB -> {
                accounts.ftb().remove(data.id);
                yield accounts.ftb().get(data.id) == null;
            }
        };

        context.reply(new Reply(data, gone));
    }

    public static class Data extends BaseData {
        public AccountProvider provider;
        /// We support either a UUID or a normal ID (sub) from keycloak
        public String id;
    }

    private static class Reply extends BaseData {
        boolean success;

        public Reply(Data data, boolean success) {
            BaseData.attachReply(data, this);
            this.success = success;
        }
    }
}
