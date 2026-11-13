package dev.ftb.app.services.accounts;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/// A signed in FTB account. Only details that aren't secret are kept here, the tokens live in the vault
public class FtbAccount {
    private final String id;
    private final String username;
    private final @Nullable String email;
    
    private final long accessExpiresAt;
    private final long refreshExpiresAt;

    private boolean notLoggedIn = false;
    
    public FtbAccount(String id, String username, @Nullable String email, long accessExpiresAt, long refreshExpiresAt) {
        this.id = id;
        this.username = username;
        this.email = email;
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

    public long getAccessExpiresAt() {
        return accessExpiresAt;
    }

    public long getRefreshExpiresAt() {
        return refreshExpiresAt;
    }

    public boolean isNotLoggedIn() {
        return notLoggedIn;
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
        ID_TOKEN("idToken"),
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
