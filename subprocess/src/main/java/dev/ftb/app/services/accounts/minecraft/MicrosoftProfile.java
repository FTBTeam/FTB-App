package dev.ftb.app.services.accounts.minecraft;

import dev.ftb.app.services.accounts.AccountTokenKey;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/// A signed in Minecraft (Microsoft) account
public class MicrosoftProfile {
    private UUID uuid;
    private String minecraftName;

    private long minecraftAccessExpiresAt;

    /// Sent to the game as the `xuid`
    private String xstsUserHash;

    private String skinUrl;
    private boolean notLoggedIn = false;

    // This one is purely for serialization and should not be used for anything else
    public MicrosoftProfile() {
    }

    public MicrosoftProfile(UUID uuid, String minecraftName, long minecraftAccessExpiresAt, String xstsUserHash, String skinUrl) {
        this.uuid = uuid;
        this.minecraftName = minecraftName;
        this.minecraftAccessExpiresAt = minecraftAccessExpiresAt;
        this.xstsUserHash = xstsUserHash;
        this.skinUrl = skinUrl;
    }

    /// Applies the result of a refresh
    public void refreshed(String minecraftName, String skinUrl, String xstsUserHash, long minecraftAccessExpiresAt) {
        this.minecraftName = minecraftName;
        this.skinUrl = skinUrl;
        this.xstsUserHash = xstsUserHash;
        this.minecraftAccessExpiresAt = minecraftAccessExpiresAt;
        this.notLoggedIn = false;
    }

    /// Used when the tokens are lost or rejected, so the user is asked to log in again.
    public void markNotLoggedIn() {
        this.notLoggedIn = true;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getMinecraftName() {
        return minecraftName;
    }

    public String getSkinUrl() {
        return skinUrl;
    }

    public String getXstsUserHash() {
        return xstsUserHash;
    }

    public long getMinecraftAccessExpiresAt() {
        return minecraftAccessExpiresAt;
    }

    public boolean isNotLoggedIn() {
        return notLoggedIn;
    }

    public SharableData toSharableData() {
        return new SharableData(this.uuid, this.minecraftName, this.skinUrl, this.notLoggedIn);
    }

    public record SharableData(
        UUID uuid,
        String minecraftName,
        String skinUrl,
        boolean notLoggedIn
    ) { }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MicrosoftProfile that = (MicrosoftProfile) o;
        return Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uuid);
    }
    
    public enum TokenKey implements AccountTokenKey {
        MINECRAFT_ACCESS_TOKEN("minecraftAccessToken"),
        MICROSOFT_REFRESH_TOKEN("microsoftRefreshToken");

        private final String keyName;

        TokenKey(String keyName) {
            this.keyName = keyName;
        }

        @Override
        public String getKeyName() {
            return keyName;
        }
    }
}
