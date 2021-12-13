package net.creeperhost.creeperlauncher.api;

import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.api.handlers.profiles.AddMcProfileHandler;
import net.creeperhost.creeperlauncher.minecraft.account.AccountManager;
import net.creeperhost.creeperlauncher.minecraft.account.AccountProfile;
import org.apache.commons.lang3.tuple.Triple;

import java.time.Instant;
import java.util.UUID;

public class UpdateMcProfileHandler implements IMessageHandler<UpdateMcProfileHandler.Data> {
  @Override
  public void handle(Data data) {
    AccountProfile.YggdrasilAuthStore mcAuth = new AccountProfile.YggdrasilAuthStore(data.accessToken, data.clientToken);
    AccountProfile profile = new AccountProfile(data.userUuid, Instant.now().toEpochMilli(), data.username, mcAuth);

    boolean removeResponse;
    Triple<Boolean, AccountProfile, UUID> addResponse;
    if (AccountManager.get().getProfiles().contains(profile)) {
      removeResponse = AccountManager.get().removeProfile(data.uuid);
      if (removeResponse) {
        addResponse = AccountManager.get().addProfile(profile);
      } else {
        addResponse = Triple.of(false, null, null);
      }
    } else {
      addResponse = Triple.of(false, null, null);
    }

    Settings.webSocketAPI.sendMessage(new UpdateMcProfileHandler.Reply(data, addResponse.getMiddle(), addResponse.getRight(), addResponse.getLeft()));

  }

  public static class Data extends AddMcProfileHandler.Data {
    public UUID uuid;
  }

  private static class Reply extends UpdateMcProfileHandler.Data {
    UUID activeProfile;
    AccountProfile profile;
    boolean success;

    public Reply(AddMcProfileHandler.Data data, AccountProfile profile, UUID activeProfile, boolean success) {
      this.requestId = data.requestId;
      this.type = data.type + "Reply";

      this.profile = profile;
      this.activeProfile = activeProfile;
      this.success = success;
    }
  }
}
