package dev.ftb.app.api.handlers.profiles;

import dev.ftb.app.services.accounts.FtbAccount;
import dev.ftb.app.services.accounts.minecraft.MicrosoftProfile;
import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.data.PrivateBaseData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GetProfilesHandler implements IMessageHandler<BaseData> {
    @Override
    public void handle(MessageContext context, BaseData data) {
        var accounts = context.services().accounts();
        context.reply(new Reply(
                data,
                accounts.minecraft().all().stream().map(MicrosoftProfile::toSharableData).collect(Collectors.toSet()),
                accounts.minecraft().active(),
                accounts.ftb().all(),
                accounts.ftb().active()
        ));
    }

    private static class Reply extends PrivateBaseData {
        Set<MicrosoftProfile.SharableData> profiles;
        MicrosoftProfile.@Nullable SharableData activeProfile;
        
        List<FtbAccount> ftbAccounts;
        @Nullable FtbAccount activeFtbAccount;

        public Reply(BaseData data, Set<MicrosoftProfile.SharableData> profiles, @Nullable MicrosoftProfile profile, List<FtbAccount> ftbAccounts, @Nullable FtbAccount activeFtbAccount) {
            this.requestId = data.requestId;
            this.type = data.type + "Reply";

            this.activeProfile = profile == null ? null : profile.toSharableData();
            this.profiles = profiles;
            this.ftbAccounts = ftbAccounts;
            this.activeFtbAccount = activeFtbAccount;
        }
    }
}
