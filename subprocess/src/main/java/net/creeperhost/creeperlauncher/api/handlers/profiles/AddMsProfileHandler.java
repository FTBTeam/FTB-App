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

      Triple<Boolean, AccountProfile, UUID> addResponse;
      if (!AccountManager.get().getProfiles().contains(profile)) {
        addResponse = AccountManager.get().addProfile(profile);
      } else {
        addResponse = Triple.of(false, null, null);
      }
      
      Settings.webSocketAPI.sendMessage(new Reply(data, addResponse.getMiddle(), addResponse.getRight(), addResponse.getLeft()));
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
