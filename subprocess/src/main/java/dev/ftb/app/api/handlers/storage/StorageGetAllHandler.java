package dev.ftb.app.api.handlers.storage;

import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;

public class StorageGetAllHandler implements IMessageHandler<BaseData> {
    @Override
    public void handle(MessageContext context, BaseData data) {
        context.reply(new Reply(data, context.services().storage().keyValue().getAllAsJson()));
    }

    static class Reply extends BaseData {
        String response;

        public Reply(BaseData data, String response) {
            this.requestId = data.requestId;
            this.type = data.type + "Reply";

            this.response = response;
        }
    }
}
