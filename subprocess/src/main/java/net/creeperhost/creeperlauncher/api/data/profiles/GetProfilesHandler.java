package net.creeperhost.creeperlauncher.api.data.profiles;

import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.minecraft.AccountManager;
import net.creeperhost.creeperlauncher.minecraft.AccountProfile;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;

public class GetProfilesHandler implements IMessageHandler<BaseData> {
    @Override
    public void handle(BaseData data) {
        Set<AccountProfile> profiles = AccountManager.get().getProfiles();
        UUID activeProfileRaw = AccountManager.get().getActiveProfileRaw();

        Settings.webSocketAPI.sendMessage(new Reply(data, profiles, activeProfileRaw));
    }

    private static class Reply extends BaseData {
        Set<AccountProfile> profiles;
        @Nullable UUID activeProfile;

        public Reply(BaseData data, Set<AccountProfile> profiles, @Nullable UUID activeProfile) {
            this.requestId = data.requestId;
            this.type = data.type + "Reply";

            this.activeProfile = activeProfile;
            this.profiles = profiles;
        }
    }
}
