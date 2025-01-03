package dev.ftb.app.api.handlers.profiles;

import dev.ftb.app.accounts.AccountManager;
import dev.ftb.app.accounts.MicrosoftProfile;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.data.PrivateBaseData;
import dev.ftb.app.api.handlers.IMessageHandler;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.stream.Collectors;

public class GetProfilesHandler implements IMessageHandler<BaseData> {
    @Override
    public void handle(BaseData data) {
        WebSocketHandler.sendMessage(new Reply(
                data,
                AccountManager.get().getProfiles().stream().map(MicrosoftProfile::toSharableData).collect(Collectors.toSet()),
                AccountManager.get().getActiveProfile()
        ));
    }

    private static class Reply extends PrivateBaseData {
        Set<MicrosoftProfile.SharableData> profiles;

        @Nullable
        MicrosoftProfile.SharableData activeProfile;

        public Reply(BaseData data, Set<MicrosoftProfile.SharableData> profiles, @Nullable MicrosoftProfile profile) {
            this.requestId = data.requestId;
            this.type = data.type + "Reply";

            this.activeProfile = profile == null ? null : profile.toSharableData();
            this.profiles = profiles;
        }
    }
}
