package net.creeperhost.creeperlauncher.api.handlers.profiles;

import net.creeperhost.creeperlauncher.accounts.AccountManager;
import net.creeperhost.creeperlauncher.accounts.MicrosoftProfile;
import net.creeperhost.creeperlauncher.api.WebSocketHandler;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
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
