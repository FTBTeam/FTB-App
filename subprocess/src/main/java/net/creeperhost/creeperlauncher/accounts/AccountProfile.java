package net.creeperhost.creeperlauncher.accounts;

import com.google.common.base.Objects;
import net.creeperhost.creeperlauncher.accounts.data.AccountSkin;
import net.creeperhost.creeperlauncher.accounts.data.MSAuthStore;

import javax.annotation.Nonnull;
import java.util.StringJoiner;
import java.util.UUID;

public class AccountProfile {
    public UUID uuid;
    public long lastLogin;

    public String username;
    
    public MSAuthStore msAuth;
    public AccountSkin[] skins;

    public AccountProfile(UUID uuid, long lastLogin, String username, AccountSkin[] skins, @Nonnull MSAuthStore msAuth) {
        this.uuid = uuid;
        this.lastLogin = lastLogin;
        this.username = username;
        this.msAuth = msAuth;
        this.skins = skins;
    }

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

    @Override
    public String toString() {
        return new StringJoiner(", ", AccountProfile.class.getSimpleName() + "[", "]")
                .add("uuid=" + uuid)
                .add("lastLogin=" + lastLogin)
                .add("username='" + username + "'")
                .add("msAuth=" + msAuth)
                .toString();
    }
}
