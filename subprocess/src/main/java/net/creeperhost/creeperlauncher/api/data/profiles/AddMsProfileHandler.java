package net.creeperhost.creeperlauncher.api.data.profiles;

import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.minecraft.AccountManager;
import net.creeperhost.creeperlauncher.minecraft.AccountProfile;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;

public class AddMsProfileHandler implements IMessageHandler<AddMsProfileHandler.Data> {
    @Override
    public void handle(Data data) {
//        Settings.webSocketAPI.sendMessage(new Reply(data, profiles, activeProfileRaw));
    }

    public static class Data extends BaseData {

    }

    private static class Reply extends Data {
        public Reply(Data data) {
            this.requestId = data.requestId;
            this.type = data.type + "Reply";
        }
    }
}
