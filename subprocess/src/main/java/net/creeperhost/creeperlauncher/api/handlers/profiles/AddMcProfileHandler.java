package net.creeperhost.creeperlauncher.api.handlers.profiles;

import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.accounts.AccountManager;
import net.creeperhost.creeperlauncher.accounts.AccountProfile;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import org.apache.commons.lang3.tuple.Triple;

import java.time.Instant;
import java.util.UUID;

public class AddMcProfileHandler implements IMessageHandler<AddMcProfileHandler.Data> {
    @Override
    public void handle(Data data) {
        AccountProfile.YggdrasilAuthStore mcAuth = new AccountProfile.YggdrasilAuthStore(data.accessToken, data.clientToken);
        AccountProfile profile = new AccountProfile(data.userUuid, Instant.now().toEpochMilli(), data.username, null, mcAuth);

        Triple<Boolean, AccountProfile, UUID> addResponse;
        if (!AccountManager.get().getProfiles().contains(profile)) {
            addResponse = AccountManager.get().addProfile(profile);
        } else {
            addResponse = Triple.of(false, null, null);
        }

        Settings.webSocketAPI.sendMessage(new Reply(data, addResponse.getMiddle(), addResponse.getRight(), addResponse.getLeft()));
    }

    public static class Data extends BaseData {
        public String username;
        public UUID userUuid;
        public String clientToken;
        public String accessToken;
    }

    private static class Reply extends Data {
        UUID activeProfile;
        AccountProfile profile;
        boolean success;

        public Reply(Data data, AccountProfile profile, UUID activeProfile, boolean success) {
            this.requestId = data.requestId;
            this.type = data.type + "Reply";

            this.profile = profile;
            this.activeProfile = activeProfile;
            this.success = success;
        }
    }
}
