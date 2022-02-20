package net.creeperhost.creeperlauncher.api.handlers.profiles;

import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.accounts.AccountManager;
import net.creeperhost.creeperlauncher.accounts.AccountProfile;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;

public class AccountIsValidHandler implements IMessageHandler<AccountIsValidHandler.Data> {
    @Override
    public void handle(Data data) {
        if (data.profileUuid == null) {
            Settings.webSocketAPI.sendMessage(new Reply(data, false, "Profile not found"));
            return;
        }

        AccountProfile profile = AccountManager.get().getProfileFromUuid(data.profileUuid);
        if (profile == null) {
            Settings.webSocketAPI.sendMessage(new Reply(data, false, "Profile not found"));
            return;
        }

        Settings.webSocketAPI.sendMessage(new Reply(data, profile.getValidator().isValid(profile), "is_valid"));
    }

    private static class Reply extends Data {
        public boolean success;
        public String response;

        public Reply(Data data, boolean success, String rawResult) {
            this.requestId = data.requestId;
            this.type = data.type + "Reply";

            this.success = success;
            this.response = rawResult;
        }
    }

    public static class Data extends BaseData {
        public String profileUuid;
    }
}
