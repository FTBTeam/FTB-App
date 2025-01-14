package dev.ftb.app.api.handlers.profiles;

import dev.ftb.app.accounts.AccountManager;
import dev.ftb.app.accounts.MicrosoftProfile;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.handlers.IMessageHandler;
import org.jetbrains.annotations.Nullable;

public class AccountIsValidHandler implements IMessageHandler<AccountIsValidHandler.Data> {
    @Override
    public void handle(Data data) {
        if (data.profileUuid == null) {
            WebSocketHandler.sendMessage(new Reply(data, MicrosoftProfile.ValidCheckResult.TOTAL_FAILURE, "Profile not found"));
            return;
        }

        MicrosoftProfile profile = AccountManager.get().getProfileFromUuid(data.profileUuid);
        if (profile == null) {
            WebSocketHandler.sendMessage(new Reply(data, MicrosoftProfile.ValidCheckResult.TOTAL_FAILURE, "Profile not found"));
            return;
        }

        WebSocketHandler.sendMessage(new Reply(data, profile.isValid(), null));
    }

    private static class Reply extends Data {
        public MicrosoftProfile.ValidCheckResult checkResult;
        
        @Nullable
        public String response;

        public Reply(Data data, MicrosoftProfile.ValidCheckResult checkResult, @Nullable String rawResult) {
            this.requestId = data.requestId;
            this.type = data.type + "Reply";

            this.checkResult = checkResult;
            this.response = rawResult;
        }
    }

    public static class Data extends BaseData {
        public String profileUuid;
    }
}
