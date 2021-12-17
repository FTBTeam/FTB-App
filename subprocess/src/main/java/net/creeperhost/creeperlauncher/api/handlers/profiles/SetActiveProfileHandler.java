package net.creeperhost.creeperlauncher.api.handlers.profiles;

import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.minecraft.account.AccountManager;
import net.creeperhost.creeperlauncher.minecraft.account.AccountProfile;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;

public class SetActiveProfileHandler implements IMessageHandler<SetActiveProfileHandler.Data> {
    @Override
    public void handle(SetActiveProfileHandler.Data data) {
        AccountManager.get().setActiveProfile(data.uuid);
        Settings.webSocketAPI.sendMessage(new Reply(
                data,
                true
        ));
    }

    public static class Data extends BaseData {
        public UUID uuid;
    }

    private static class Reply extends BaseData {
        boolean success;

        public Reply(BaseData data, boolean success) {
            this.requestId = data.requestId;
            this.type = data.type + "Reply";

            this.success = success;
        }
    }
}
