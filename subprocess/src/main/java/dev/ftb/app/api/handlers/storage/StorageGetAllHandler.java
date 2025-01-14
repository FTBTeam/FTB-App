package dev.ftb.app.api.handlers.storage;

import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.storage.KVStorage;

public class StorageGetAllHandler implements IMessageHandler<BaseData> {
    @Override
    public void handle(BaseData data) {
        WebSocketHandler.sendMessage(new Reply(data, KVStorage.getInstance().getAllAsJson()));
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
