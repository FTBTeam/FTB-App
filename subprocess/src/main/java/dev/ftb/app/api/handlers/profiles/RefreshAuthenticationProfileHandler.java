package dev.ftb.app.api.handlers.profiles;

import dev.ftb.app.accounts.AccountManager;
import dev.ftb.app.accounts.MicrosoftProfile;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.data.PrivateBaseData;
import dev.ftb.app.api.handlers.IMessageHandler;

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
        
        refreshMicrosoft(data, profile);
    }

    /**
     * Refresh using Microsoft flow
     */
    private void refreshMicrosoft(Data data, MicrosoftProfile profile) {
        var result = profile.refreshAccount();
        if (!result) {
            WebSocketHandler.sendMessage(new Reply(data, false, "Failed to refresh account"));
            return;
        }
        
        WebSocketHandler.sendMessage(new Reply(data, true, ""));
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
