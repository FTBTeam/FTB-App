package net.creeperhost.creeperlauncher.api.handlers.profiles;

import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.minecraft.account.AccountManager;
import net.creeperhost.creeperlauncher.minecraft.account.AccountProfile;
import org.apache.commons.lang3.tuple.Triple;

import java.time.Instant;
import java.util.UUID;

public class AddMsProfileHandler implements IMessageHandler<AddMsProfileHandler.Data> {
    @Override
    public void handle(Data data) {
      AccountProfile.MSAuthStore msAccount = new AccountProfile.MSAuthStore(data);
      AccountProfile profile = new AccountProfile(data.minecraftUuid, Instant.now().getEpochSecond(), data.minecraftName, msAccount);

      Reply reply;
      if (!AccountManager.get().getProfiles().contains(profile)) {
        // Quickly validate that the users UUID isn't already in use in a Minecraft account
        if (profile.isMicrosoft) {
          // Find the first account that has the same UUID but is not Microsoft and remove it if it's found
          AccountManager.get().getProfiles().stream().filter(
              p -> !p.isMicrosoft && p.uuid.equals(profile.uuid)
          ).findFirst().ifPresent(e -> {
            AccountManager.get().removeProfile(e.uuid);
          });
        }

        Triple<Boolean, AccountProfile, UUID> profileAction = AccountManager.get().addProfile(profile);
        reply = new Reply(data, profileAction.getMiddle(), profileAction.getRight(), profileAction.getLeft(), "completed");
      } else {
        reply = new Reply(data, null, null, false, "profile_exists");
      }
      
      Settings.webSocketAPI.sendMessage(reply);
    }

    public static class Data extends BaseData {
      public String minecraftName;
      public UUID minecraftUuid;
      public String minecraftToken;
      public String xstsUserHash;
      public String xstsToken;
      public String xblUserHash;
      public String xblToken;
      public String liveAccessToken;
      public String liveRefreshToken; // This is needed for the account refresh
      public String liveExpires;
      public String liveExpiresAt;
    }

  private static class Reply extends Data {
    UUID activeProfile;
    AccountProfile profile;
    String status;
    boolean success;

    public Reply(Data data, AccountProfile profile, UUID activeProfile, boolean success, String status) {
      this.requestId = data.requestId;
      this.type = data.type + "Reply";

      this.profile = profile;
      this.activeProfile = activeProfile;
      this.success = success;
      this.status = status;
    }
  }
}
