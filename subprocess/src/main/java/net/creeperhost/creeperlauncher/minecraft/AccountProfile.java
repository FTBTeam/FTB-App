package net.creeperhost.creeperlauncher.minecraft;


import com.google.gson.annotations.JsonAdapter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.StringJoiner;
import java.util.UUID;

public class AccountProfile {
    // TODO: this is kinda ugly
    public boolean isMicrosoft = false;
    public UUID uuid;
    public long lastLogin;

    public String username;
    public UUID userUuid;

    @Nullable
    public MSAuthStore msAuth = null;

    @Nullable
    public YggdrasilAuthStore mcAuth = null;

    public AccountProfile(long lastLogin, String username, UUID userUuid, @Nonnull MSAuthStore msAuth) {
        this(true, UUID.randomUUID(), lastLogin, username, userUuid, msAuth);
    }

    public AccountProfile(long lastLogin, String username, UUID userUuid, @Nonnull YggdrasilAuthStore mcAuth) {
        this(false, UUID.randomUUID(), lastLogin, username, userUuid, mcAuth);
    }

    public AccountProfile(boolean isMicrosoft, UUID uuid, long lastLogin, String username, UUID userUuid, @Nonnull MSAuthStore msAuth) {
        this.isMicrosoft = isMicrosoft;
        this.uuid = uuid;
        this.lastLogin = lastLogin;
        this.username = username;
        this.userUuid = userUuid;
        this.msAuth = msAuth;
    }

    public AccountProfile(boolean isMicrosoft, UUID uuid, long lastLogin, String username, UUID userUuid, @Nonnull YggdrasilAuthStore msAuth) {
        this.isMicrosoft = isMicrosoft;
        this.uuid = uuid;
        this.lastLogin = lastLogin;
        this.username = username;
        this.userUuid = userUuid;
        this.mcAuth = msAuth;
    }

    public static class MSAuthStore {
        public String refreshToken;
        public String clientId;
        public String xuid;

        public MSAuthStore(String refreshToken, String clientId, String xuid) {
            this.refreshToken = refreshToken;
            this.clientId = clientId;
            this.xuid = xuid;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", MSAuthStore.class.getSimpleName() + "[", "]")
                    .add("refreshToken='" + refreshToken + "'")
                    .add("clientId='" + clientId + "'")
                    .add("xuid='" + xuid + "'")
                    .toString();
        }
    }

    public static class YggdrasilAuthStore {
        public UUID clientId;
        public String accessToken;

        public YggdrasilAuthStore(UUID clientId, String accessToken) {
            this.clientId = clientId;
            this.accessToken = accessToken;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", YggdrasilAuthStore.class.getSimpleName() + "[", "]")
                    .add("clientId=" + clientId)
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
                .add("userUuid=" + userUuid)
                .add("msAuth=" + msAuth)
                .add("mcAuth=" + mcAuth)
                .toString();
    }
}
