package net.creeperhost.creeperlauncher.api.handlers.storage;

import net.creeperhost.creeperlauncher.api.WebSocketHandler;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.storage.KVStorage;

import javax.annotation.Nullable;

public class StorageGetHandler implements IMessageHandler<StorageGetHandler.Data> {
    @Override
    public void handle(Data data) {
        if (data.key == null || data.key.isEmpty()) {
            WebSocketHandler.sendMessage(new Reply(data, null));
            return;
        }

        WebSocketHandler.sendMessage(new Reply(data, KVStorage.getInstance().getValue(data.key)));
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
