package net.creeperhost.creeperlauncher.minecraft.account;

import com.google.common.base.Objects;
import net.creeperhost.creeperlauncher.api.handlers.profiles.AddMsProfileHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.StringJoiner;
import java.util.UUID;

public class AccountProfile {
    public boolean isMicrosoft;
    public UUID uuid;
    public long lastLogin;

    public String username;

    @Nullable
    public MSAuthStore msAuth = null;

    @Nullable
    public YggdrasilAuthStore mcAuth = null;

    public AccountProfile(UUID playerUuid, long lastLogin, String username, @Nonnull MSAuthStore msAuth) {
        this(true, playerUuid, lastLogin, username, msAuth);
    }

    public AccountProfile(UUID playerUuid, long lastLogin, String username, @Nonnull YggdrasilAuthStore mcAuth) {
        this(false, playerUuid, lastLogin, username, mcAuth);
    }

    public AccountProfile(boolean isMicrosoft, UUID uuid, long lastLogin, String username, @Nonnull MSAuthStore msAuth) {
        this.isMicrosoft = isMicrosoft;
        this.uuid = uuid;
        this.lastLogin = lastLogin;
        this.username = username;
        this.msAuth = msAuth;
    }

    public AccountProfile(boolean isMicrosoft, UUID uuid, long lastLogin, String username, @Nonnull YggdrasilAuthStore msAuth) {
        this.isMicrosoft = isMicrosoft;
        this.uuid = uuid;
        this.lastLogin = lastLogin;
        this.username = username;
        this.mcAuth = msAuth;
    }

    /**
     * Only equals if the uuids are equal. The rest of the info here is meaningless if the UUIDs are the same.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountProfile that = (AccountProfile) o;
        return Objects.equal(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uuid);
    }

    public static class MSAuthStore {
      public UUID minecraftUuid;
      public String minecraftToken;
      public String xstsUserHash;
      public String xstsToken;
      public String xblUserHash;
      public String xblToken;
      public String liveAccessToken;
      public String liveRefreshToken; // This is needed for the account refresh
      public String liveExpires;
      
      public MSAuthStore(AddMsProfileHandler.Data data) {
        this.minecraftUuid = data.minecraftUuid; 
        this.minecraftToken = data.minecraftToken; 
        this.xstsUserHash = data.xstsUserHash; 
        this.xstsToken = data.xstsToken; 
        this.xblUserHash = data.xblUserHash; 
        this.xblToken = data.xblToken; 
        this.liveAccessToken = data.liveAccessToken; 
        this.liveRefreshToken = data.liveRefreshToken; // This is needed for the account refresh 
        this.liveExpires = data.liveExpires; 
      }

      @Override
      public String toString() {
        return new StringJoiner(", ", MSAuthStore.class.getSimpleName() + "[", "]")
          .add("minecraftUuid=" + minecraftUuid)
          .add("minecraftToken='" + minecraftToken + "'")
          .add("xstsUserHash='" + xstsUserHash + "'")
          .add("xstsToken='" + xstsToken + "'")
          .add("xblUserHash='" + xblUserHash + "'")
          .add("xblToken='" + xblToken + "'")
          .add("liveAccessToken='" + liveAccessToken + "'")
          .add("liveRefreshToken='" + liveRefreshToken + "'")
          .add("liveExpires='" + liveExpires + "'")
          .toString();
      }
    }

    public static class YggdrasilAuthStore {
        public String clientToken;
        public String accessToken;

        public YggdrasilAuthStore(String accessToken, String clientToken) {
            this.clientToken = clientToken;
            this.accessToken = accessToken;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", YggdrasilAuthStore.class.getSimpleName() + "[", "]")
                    .add("clientToken=" + clientToken)
                    .add("accessToken='" + accessToken + "'")
                    .toString();
        }
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AccountProfile.class.getSimpleName() + "[", "]")
                .add("isMicrosoft=" + isMicrosoft)
                .add("uuid=" + uuid)
                .add("lastLogin=" + lastLogin)
                .add("username='" + username + "'")
                .add("msAuth=" + msAuth)
                .add("mcAuth=" + mcAuth)
                .toString();
    }
}
