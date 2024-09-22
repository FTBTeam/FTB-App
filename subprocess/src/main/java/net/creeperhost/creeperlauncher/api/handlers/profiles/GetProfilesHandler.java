package net.creeperhost.creeperlauncher.api.handlers.profiles;

import net.creeperhost.creeperlauncher.accounts.AccountManager;
import net.creeperhost.creeperlauncher.accounts.MicrosoftProfile;
import net.creeperhost.creeperlauncher.api.WebSocketHandler;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.data.PrivateBaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;

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
