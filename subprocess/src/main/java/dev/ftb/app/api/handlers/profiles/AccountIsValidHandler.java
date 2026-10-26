package dev.ftb.app.api.handlers.profiles;

import dev.ftb.app.AppMain;
import dev.ftb.app.accounts.MicrosoftProfile;
import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;
import org.jspecify.annotations.Nullable;

public class AccountIsValidHandler implements IMessageHandler<AccountIsValidHandler.Data> {
    @Override
    public void handle(MessageContext context, Data data) {
        if (data.profileUuid == null) {
            context.reply(new Reply(data, MicrosoftProfile.ValidCheckResult.TOTAL_FAILURE, "Profile not found"));
            return;
        }

        MicrosoftProfile profile = AppMain.instance().services().accounts().getProfileFromUuid(data.profileUuid);
        if (profile == null) {
            context.reply(new Reply(data, MicrosoftProfile.ValidCheckResult.TOTAL_FAILURE, "Profile not found"));
            return;
        }

        context.reply(new Reply(data, profile.isValid(), null));
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
