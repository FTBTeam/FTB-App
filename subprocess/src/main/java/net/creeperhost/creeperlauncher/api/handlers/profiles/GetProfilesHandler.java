package net.creeperhost.creeperlauncher.api.handlers.profiles;

import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.accounts.AccountManager;
import net.creeperhost.creeperlauncher.accounts.AccountProfile;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;


import javax.annotation.Nullable;
import java.util.Set;

public class GetProfilesHandler implements IMessageHandler<BaseData> {
    @Override
    public void handle(BaseData data) {
        Settings.webSocketAPI.sendMessage(new Reply(
                data,
                AccountManager.get().getProfiles(),
                AccountManager.get().getActiveProfile()
        ));
    }

    private static class Reply extends BaseData {
        Set<AccountProfile> profiles;

        @Nullable
        AccountProfile activeProfile;

        public Reply(BaseData data, Set<AccountProfile> profiles, @Nullable AccountProfile activeProfile) {
            this.requestId = data.requestId;
            this.type = data.type + "Reply";

            this.activeProfile = activeProfile;
            this.profiles = profiles;
        }
    }
}
