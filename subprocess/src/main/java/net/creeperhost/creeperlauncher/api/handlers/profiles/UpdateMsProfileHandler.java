package net.creeperhost.creeperlauncher.api.handlers.profiles;

import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.accounts.AccountManager;
import net.creeperhost.creeperlauncher.accounts.AccountProfile;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import org.apache.commons.lang3.tuple.Triple;

import java.time.Instant;
import java.util.UUID;

public class UpdateMsProfileHandler implements IMessageHandler<UpdateMsProfileHandler.Data> {
    @Override
    public void handle(UpdateMsProfileHandler.Data data) {
        AccountProfile.MSAuthStore msAccount = new AccountProfile.MSAuthStore(data);
        AccountProfile profile = new AccountProfile(data.minecraftUuid, Instant.now().getEpochSecond(), data.minecraftName, null, msAccount);

        boolean removeResponse;
        Triple<Boolean, AccountProfile, UUID> addResponse;
        if (AccountManager.get().getProfiles().contains(profile)) {
            removeResponse = AccountManager.get().removeProfile(data.uuid);
            if (removeResponse) {
                System.out.println("Adding profile again");
                addResponse = AccountManager.get().addProfile(profile);
            } else {
                System.out.println("Couldn't remove the profile");
                addResponse = Triple.of(false, null, null);
            }
        } else {
            System.out.println("Profile didn't exist");
            addResponse = Triple.of(false, null, null);
        }

        Settings.webSocketAPI.sendMessage(new UpdateMsProfileHandler.Reply(data, addResponse.getMiddle(), addResponse.getRight(), addResponse.getLeft()));
    }

    public static class Data extends AddMsProfileHandler.Data {
        public UUID uuid;
    }

    private static class Reply extends UpdateMsProfileHandler.Data {
        UUID activeProfile;
        AccountProfile profile;
        boolean success;

        public Reply(UpdateMsProfileHandler.Data data, AccountProfile profile, UUID activeProfile, boolean success) {
            this.requestId = data.requestId;
            this.type = data.type + "Reply";

            this.profile = profile;
            this.activeProfile = activeProfile;
            this.success = success;
        }
    }
}
