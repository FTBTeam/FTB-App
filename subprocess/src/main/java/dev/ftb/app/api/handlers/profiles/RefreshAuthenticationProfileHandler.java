package dev.ftb.app.api.handlers.profiles;

import dev.ftb.app.accounts.MicrosoftProfile;
import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.data.PrivateBaseData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;

public class RefreshAuthenticationProfileHandler implements IMessageHandler<RefreshAuthenticationProfileHandler.Data> {
    @Override
    public void handle(MessageContext context, Data data) {
        if (data.profileUuid == null) {
            context.reply(new Reply(data, false, "Profile not found"));
            return;
        }

        MicrosoftProfile profile = context.services().accounts().getProfileFromUuid(data.profileUuid);
        if (profile == null) {
            context.reply(new Reply(data, false, "Profile not found"));
            return;
        }
        
        refreshMicrosoft(context, data, profile);
    }

    /**
     * Refresh using Microsoft flow
     */
    private void refreshMicrosoft(MessageContext context, Data data, MicrosoftProfile profile) {
        var result = profile.refreshAccount(context.services().http(), context.services().accounts());
        if (!result) {
            context.reply(new Reply(data, false, "Failed to refresh account"));
            return;
        }
        
        context.reply(new Reply(data, true, ""));
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
