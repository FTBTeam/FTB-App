package dev.ftb.app.services.accounts;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/// A signed in FTB account. Only details that aren't secret are kept here, the tokens live in the vault
public class FtbAccount {
    private final String id;
    private String username;
    private @Nullable String email;
    private @Nullable String avatarUrl;
    private boolean premium;
    
    private long accessExpiresAt;
    private long refreshExpiresAt;

    private boolean notLoggedIn = false;
    
    public FtbAccount(FtbTokenClaims claims, long accessExpiresAt, long refreshExpiresAt) {
        this.id = claims.id();
        this.applyClaims(claims);
        this.accessExpiresAt = accessExpiresAt;
        this.refreshExpiresAt = refreshExpiresAt;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public @Nullable String getEmail() {
        return email;
    }

    public @Nullable String getAvatarUrl() {
        return avatarUrl;
    }

    public boolean isPremium() {
        return premium;
    }

    public long getAccessExpiresAt() {
        return accessExpiresAt;
    }

    public long getRefreshExpiresAt() {
        return refreshExpiresAt;
    }

    public boolean isNotLoggedIn() {
        return notLoggedIn;
    }
    
    public void refreshed(@Nullable FtbTokenClaims claims, long accessExpiresAt, long refreshExpiresAt) {
        if (claims != null && claims.id().equals(this.id)) {
            this.applyClaims(claims);
        }
        this.accessExpiresAt = accessExpiresAt;
        this.refreshExpiresAt = refreshExpiresAt;
    }

    private void applyClaims(FtbTokenClaims claims) {
        this.username = claims.username();
        this.email = claims.email();
        this.avatarUrl = claims.avatarUrl();
        this.premium = claims.premium();
    }

    /// Used when the tokens are lost or can no longer be refreshed, so the user is asked to log in again.
    public void markNotLoggedIn() {
        this.notLoggedIn = true;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        FtbAccount that = (FtbAccount) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public enum TokenKey implements AccountTokenKey {
        ACCESS_TOKEN("accessToken"),
        REFRESH_TOKEN("refreshToken");

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
