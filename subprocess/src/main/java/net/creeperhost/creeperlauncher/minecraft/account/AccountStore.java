package net.creeperhost.creeperlauncher.minecraft.account;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;

public class AccountStore {
    public Set<AccountProfile> profiles;

    @Nullable
    public UUID activeProfile;

    public AccountStore(Set<AccountProfile> profiles, @Nullable UUID activeProfile) {
        this.profiles = profiles;
        this.activeProfile = activeProfile;
    }
}
