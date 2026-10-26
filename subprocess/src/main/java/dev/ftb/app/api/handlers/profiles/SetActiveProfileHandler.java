package dev.ftb.app.api.handlers.profiles;

import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.data.PrivateBaseData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;

import java.util.UUID;

public class SetActiveProfileHandler implements IMessageHandler<SetActiveProfileHandler.Data> {
    @Override
    public void handle(MessageContext context, Data data) {
        context.services().accounts().setActiveProfile(data.uuid);
        context.reply(new Reply(
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
