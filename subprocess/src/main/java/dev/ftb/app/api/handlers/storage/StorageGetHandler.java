package dev.ftb.app.api.handlers.storage;

import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;
import org.jspecify.annotations.Nullable;

public class StorageGetHandler implements IMessageHandler<StorageGetHandler.Data> {
    @Override
    public void handle(MessageContext context, Data data) {
        if (data.key == null || data.key.isEmpty()) {
            context.reply(new Reply(data, null));
            return;
        }

        context.reply(new Reply(data, context.services().storage().keyValue().get(data.key)));
    }

    public static class Data extends BaseData {
        @Nullable String key;
    }

    private static class Reply extends Data {
        String response;

        public Reply(Data data, String response) {
            this.requestId = data.requestId;
            this.type = data.type + "Reply";

            this.response = response;
        }
    }
}
