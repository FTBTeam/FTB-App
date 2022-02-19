package net.creeperhost.creeperlauncher.accounts;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;

public class AccountStore {
    String version;

    public Set<AccountProfile> profiles;

    @Nullable
    public UUID activeProfile;

    public AccountStore(Set<AccountProfile> profiles, @Nullable UUID activeProfile) {
        this.version = AccountManager.SPEC_VERSION;
        this.profiles = profiles;
        this.activeProfile = activeProfile;
    }
}
