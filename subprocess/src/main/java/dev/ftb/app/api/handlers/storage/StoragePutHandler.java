package dev.ftb.app.api.handlers.storage;

import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;
import dev.ftb.app.storage.KVStorage;
import org.jspecify.annotations.Nullable;

public class StoragePutHandler implements IMessageHandler<StoragePutHandler.Data> {
    @Override
    public void handle(MessageContext context, Data data) {
        if (data.key == null || data.key.isEmpty()) {
            context.reply(new Reply(data, false));
            return;
        }

        var success = KVStorage.getInstance().put(data.key, data.value);
        context.reply(new Reply(data, success));
    }

    public static class Data extends BaseData {
        @Nullable String key;
        @Nullable String value;
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
