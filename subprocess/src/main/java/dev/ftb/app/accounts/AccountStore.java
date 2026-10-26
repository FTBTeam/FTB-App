package dev.ftb.app.accounts;

import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AccountStore {
    public static final AccountStore EMPTY = new AccountStore(new HashSet<>(), null);
    
    String version;

    public Set<MicrosoftProfile> profiles;

    @Nullable
    public UUID activeProfile;

    public AccountStore(Set<MicrosoftProfile> profiles, @Nullable UUID activeProfile) {
        this.version = AccountManager.SPEC_VERSION;
        this.profiles = profiles;
        this.activeProfile = activeProfile;
    }
}
