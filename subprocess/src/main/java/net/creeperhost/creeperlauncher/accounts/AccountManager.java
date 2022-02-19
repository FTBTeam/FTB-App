package net.creeperhost.creeperlauncher.accounts;

import com.google.gson.Gson;
import net.creeperhost.creeperlauncher.Constants;
import org.apache.commons.lang3.tuple.Triple;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class AccountManager {
    public static final String SPEC_VERSION = "1.0.0";

    private static final AccountManager ACCOUNT_MANAGER = new AccountManager();
    private static final String STORE_FILE = "profiles.json";

    private Set<AccountProfile> profiles = new HashSet<>();

    @Nullable
    private AccountProfile activeProfile = null;

    private AccountManager() {
        loadProfiles();
    }

    public static AccountManager get() {
        return ACCOUNT_MANAGER;
    }

    public boolean removeProfile(UUID uuid) {
        boolean removed = this.profiles.stream().filter(p -> p.uuid.equals(uuid)).findFirst()
                .map(e -> this.profiles.remove(e))
                .orElse(false);

        if (this.activeProfile != null && this.activeProfile.uuid.equals(uuid)) {
            this.activeProfile = this.profiles.stream().findFirst().orElse(null);
        }

        if (removed) {
            this.saveProfiles();
        }

        return removed;
    }

    public void setActiveProfile(UUID uuid) {
        this.activeProfile = this.profiles.stream().filter(p -> p.uuid.equals(uuid)).findFirst().orElse(null);
        this.saveProfiles();
    }

    public Triple<Boolean, AccountProfile, UUID> addProfile(AccountProfile profile) {
        this.profiles.add(profile);
        this.activeProfile = profile;
        this.saveProfiles();

        return Triple.of(this.profiles.stream().anyMatch(e -> e.uuid == profile.uuid), profile, profile.uuid);
    }

    // Load profiles from json structure
    public void loadProfiles() {
        if (!Files.exists(Constants.getDataDir().resolve(STORE_FILE))) {
            return;
        }

        try {
            // Get the profile data from the json file.
            AccountStore store = new Gson().fromJson(Files.readString(Constants.getDataDir().resolve(STORE_FILE)), AccountStore.class);

            // TODO: create a migration system if this ever gets changed again.
            //       For this implementation, we actually want it to drop the old data as it's likely fucked anyway.
            if (!Objects.equals(store.version, SPEC_VERSION)) {
                // Allow the system to create a new one
                return;
            }

            this.profiles = store.profiles;
            this.activeProfile = store.profiles.stream()
                    .filter(e -> e.uuid.equals(store.activeProfile))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Save profiles to json structure
    public void saveProfiles() {
        try {
            AccountStore store = new AccountStore(this.profiles, this.activeProfile != null ? this.activeProfile.uuid : null);
            Files.writeString(Constants.getDataDir().resolve(STORE_FILE), new Gson().toJson(store, AccountStore.class));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Set<AccountProfile> getProfiles() {
        return profiles;
    }

    @Nullable
    public AccountProfile getActiveProfile() {
        return this.activeProfile;
    }
}
