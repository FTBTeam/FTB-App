package net.creeperhost.creeperlauncher.api.data.profiles;

import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.minecraft.AccountManager;

import java.util.UUID;

public class RemoveProfileHandler implements IMessageHandler<RemoveProfileHandler.Data> {
    @Override
    public void handle(Data data) {
        Settings.webSocketAPI.sendMessage(new Reply(data, AccountManager.get().removeProfile(data.uuid)));
    }

    public static class Data extends BaseData {
        UUID uuid;
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
