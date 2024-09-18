package net.creeperhost.creeperlauncher.api.handlers.profiles;

import net.creeperhost.creeperlauncher.accounts.AccountManager;
import net.creeperhost.creeperlauncher.accounts.MicrosoftProfile;
import net.creeperhost.creeperlauncher.api.WebSocketHandler;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.data.PrivateBaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;

public class RefreshAuthenticationProfileHandler implements IMessageHandler<RefreshAuthenticationProfileHandler.Data> {
    @Override
    public void handle(Data data) {
        if (data.profileUuid == null) {
            WebSocketHandler.sendMessage(new Reply(data, false, "Profile not found"));
            return;
        }

        MicrosoftProfile profile = AccountManager.get().getProfileFromUuid(data.profileUuid);
        if (profile == null) {
            WebSocketHandler.sendMessage(new Reply(data, false, "Profile not found"));
            return;
        }
        
        refreshMicrosoft(profile);
    }

    /**
     * Refresh using Microsoft flow
     */
    private void refreshMicrosoft(MicrosoftProfile profile) {
        var result = profile.refreshAccount();
        if (!result) {
            WebSocketHandler.sendMessage(new Reply(new Data(), false, "Failed to refresh account"));
            return;
        }
        
        WebSocketHandler.sendMessage(new Reply(new Data(), true, ""));
    }

    private static class Reply extends PrivateBaseData {
        public boolean success;
        public String code;

        public Reply(Data data, boolean success, String code) {
            this.requestId = data.requestId;
            this.type = data.type + "Reply";

            this.success = success;
            this.code = code;
        }
    }

    public static class Data extends BaseData {
        public String profileUuid;
    }
}
