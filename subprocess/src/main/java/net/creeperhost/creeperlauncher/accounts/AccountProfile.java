package net.creeperhost.creeperlauncher.accounts;

import com.google.common.base.Objects;
import net.creeperhost.creeperlauncher.accounts.authentication.AuthenticatorValidator;
import net.creeperhost.creeperlauncher.accounts.authentication.MicrosoftAuthenticator;
import net.creeperhost.creeperlauncher.accounts.authentication.MojangAuthenticator;
import net.creeperhost.creeperlauncher.accounts.stores.AccountSkin;
import net.creeperhost.creeperlauncher.accounts.stores.MSAuthStore;
import net.creeperhost.creeperlauncher.accounts.stores.YggdrasilAuthStore;

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
    public MSAuthStore msAuth;

    @Nullable
    public YggdrasilAuthStore mcAuth;
    public AccountSkin[] skins;

    public AccountProfile(UUID uuid, long lastLogin, String username, AccountSkin[] skins, @Nonnull MSAuthStore msAuth) {
        this(true, uuid, lastLogin, username, msAuth, null, skins);
    }

    public AccountProfile(UUID uuid, long lastLogin, String username, AccountSkin[] skins, @Nonnull YggdrasilAuthStore mcAuth) {
        this(false, uuid, lastLogin, username, null, mcAuth, skins);
    }

    public AccountProfile(boolean isMicrosoft, UUID uuid, long lastLogin, String username, @Nullable MSAuthStore msAuth, @Nullable YggdrasilAuthStore mcAuth, AccountSkin[] skins) {
        this.isMicrosoft = isMicrosoft;
        this.uuid = uuid;
        this.lastLogin = lastLogin;
        this.username = username;
        this.msAuth = msAuth;
        this.mcAuth = mcAuth;
        this.skins = skins;
    }

    public AuthenticatorValidator<?, ?, ?> getValidator() {
        return this.isMicrosoft ? new MicrosoftAuthenticator() : new MojangAuthenticator();
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
