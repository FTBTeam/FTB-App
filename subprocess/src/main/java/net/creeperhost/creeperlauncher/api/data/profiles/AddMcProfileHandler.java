package net.creeperhost.creeperlauncher.api.data.profiles;

import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.minecraft.AccountManager;
import net.creeperhost.creeperlauncher.minecraft.AccountProfile;
import org.apache.commons.lang3.tuple.Triple;

import java.time.Instant;
import java.util.UUID;

public class AddMcProfileHandler implements IMessageHandler<AddMcProfileHandler.Data> {
    @Override
    public void handle(Data data) {
        AccountProfile.YggdrasilAuthStore mcAuth = new AccountProfile.YggdrasilAuthStore(data.clientId, data.accessToken);
        AccountProfile profile = new AccountProfile(Instant.now().toEpochMilli(), data.username, data.userUuid, mcAuth);

        // TODO: don't allow duplication
        Triple<Boolean, AccountProfile, UUID> addResponse = AccountManager.get().addProfile(profile);

        Settings.webSocketAPI.sendMessage(new Reply(data, addResponse.getMiddle(), addResponse.getRight(), addResponse.getLeft()));
    }

    public static class Data extends BaseData {
        public String username;
        public UUID userUuid;
        public UUID clientId;
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
