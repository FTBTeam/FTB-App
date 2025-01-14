package dev.ftb.app.accounts.data;

import java.util.StringJoiner;
import java.util.UUID;

public class MSAuthStore {
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
