package dev.ftb.app.structs.accounts;

import dev.ftb.app.services.accounts.minecraft.MicrosoftProfile;
import dev.ftb.app.services.accounts.McAccounts;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AccountStore {
    /// A fresh store each time, as the profiles set is mutated in place.
    public static AccountStore empty() {
        return new AccountStore(new HashSet<>(), null);
    }
    
    String version;

    public Set<MicrosoftProfile> profiles;

    @Nullable
    public UUID activeProfile;

    public AccountStore(Set<MicrosoftProfile> profiles, @Nullable UUID activeProfile) {
        this.version = McAccounts.SPEC_VERSION;
        this.profiles = profiles;
        this.activeProfile = activeProfile;
    }
}
