package net.creeperhost.creeperlauncher.accounts;

import com.google.common.base.Objects;
import net.creeperhost.creeperlauncher.api.handlers.profiles.AddMsProfileHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.StringJoiner;
import java.util.UUID;

public class AccountProfile {
    public boolean isMicrosoft;
    public UUID uuid;
    public long lastLogin;

    public String username;

    @Nullable
    public MSAuthStore msAuth;

    @Nullable
    public YggdrasilAuthStore mcAuth;
    public AccountSkins skins;

    public AccountProfile(UUID uuid, long lastLogin, String username, AccountSkins skins, @Nonnull MSAuthStore msAuth) {
        this(true, uuid, lastLogin, username, msAuth, null, skins);
    }

    public AccountProfile(UUID uuid, long lastLogin, String username, AccountSkins skins, @Nonnull YggdrasilAuthStore mcAuth) {
        this(false, uuid, lastLogin, username, null, mcAuth, skins);
    }

    public AccountProfile(boolean isMicrosoft, UUID uuid, long lastLogin, String username, @Nullable MSAuthStore msAuth, @Nullable YggdrasilAuthStore mcAuth, AccountSkins skins) {
        this.isMicrosoft = isMicrosoft;
        this.uuid = uuid;
        this.lastLogin = lastLogin;
        this.username = username;
        this.msAuth = msAuth;
        this.mcAuth = mcAuth;
        this.skins = skins;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountProfile that = (AccountProfile) o;
        return isMicrosoft == that.isMicrosoft && Objects.equal(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(isMicrosoft, uuid);
    }

    public static class MSAuthStore {
        public UUID minecraftUuid;
        public String minecraftToken;
        public String xblUserHash;
        public String liveAccessToken;
        public String liveRefreshToken; // This is needed for the account refresh
        public long liveExpiresAt;

        public MSAuthStore(UUID minecraftUuid, String minecraftToken, String xblUserHash, String liveAccessToken, String liveRefreshToken, long liveExpiresAt) {
            this.minecraftUuid = minecraftUuid;
            this.minecraftToken = minecraftToken;
            this.xblUserHash = xblUserHash;
            this.liveAccessToken = liveAccessToken;
            this.liveRefreshToken = liveRefreshToken;
            this.liveExpiresAt = liveExpiresAt;
        }

        public MSAuthStore(AddMsProfileHandler.Data data) {
            this(data.minecraftUuid, data.minecraftToken, data.xblUserHash, data.liveAccessToken, data.liveRefreshToken, Instant.now().getEpochSecond() + Integer.parseInt(data.liveExpires));
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", MSAuthStore.class.getSimpleName() + "[", "]")
                    .add("minecraftUuid=" + minecraftUuid)
                    .add("minecraftToken='" + minecraftToken + "'")
                    .add("xblUserHash='" + xblUserHash + "'")
                    .add("liveAccessToken='" + liveAccessToken + "'")
                    .add("liveRefreshToken='" + liveRefreshToken + "'")
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
