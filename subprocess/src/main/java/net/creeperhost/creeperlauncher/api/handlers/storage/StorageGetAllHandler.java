package net.creeperhost.creeperlauncher.api.handlers.storage;

import net.creeperhost.creeperlauncher.GenericStorage;
import net.creeperhost.creeperlauncher.api.WebSocketHandler;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;

public class StorageGetAllHandler implements IMessageHandler<BaseData> {
    @Override
    public void handle(BaseData data) {
        WebSocketHandler.sendMessage(new Reply(data, GenericStorage.getInstance().getAllAsJson()));
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
