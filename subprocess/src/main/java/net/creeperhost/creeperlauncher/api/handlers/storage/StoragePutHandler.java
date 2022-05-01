package net.creeperhost.creeperlauncher.api.handlers.storage;

import net.creeperhost.creeperlauncher.GenericStorage;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;

import javax.annotation.Nullable;

public class StoragePutHandler implements IMessageHandler<StoragePutHandler.Data> {
    @Override
    public void handle(Data data) {
        if (data.key == null || data.key.isEmpty()) {
            Settings.webSocketAPI.sendMessage(new Reply(data, false));
            return;
        }

        var success = GenericStorage.getInstance().put(data.key, data.value);
        Settings.webSocketAPI.sendMessage(new Reply(data, success));
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
