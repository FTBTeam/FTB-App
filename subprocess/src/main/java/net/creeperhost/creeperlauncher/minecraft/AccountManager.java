package net.creeperhost.creeperlauncher.minecraft;

import com.google.gson.Gson;
import net.creeperhost.creeperlauncher.Constants;
import org.apache.commons.lang3.tuple.Triple;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AccountManager {
    private static final AccountManager ACCOUNT_MANAGER = new AccountManager();
    private static final String STORE_FILE = "profiles.json";

    private Set<AccountProfile> profiles = new HashSet<>();
    private UUID activeProfile;

    static {
        ACCOUNT_MANAGER.loadProfiles();
    }

    public AccountManager() {}

    public static AccountManager get() {
        return ACCOUNT_MANAGER;
    }

    public boolean removeProfile(UUID uuid) {
        boolean removed = this.profiles.stream().filter(p -> p.uuid.equals(uuid)).findFirst()
                .map(e -> this.profiles.remove(e))
                .orElse(false);

        if (removed) {
            this.saveProfiles();
        }

        return removed;
    }

    public Triple<Boolean, AccountProfile, UUID> addProfile(AccountProfile profile) {
        this.profiles.add(profile);
        this.activeProfile = profile.uuid;
        this.saveProfiles();

        return Triple.of(this.profiles.stream().anyMatch(e -> e.uuid == profile.uuid), profile, profile.uuid);
    }

    // Load profiles from json structure
    public void loadProfiles() {
        if (!Files.exists(Constants.getDataDir().resolve(STORE_FILE))) {
            return;
        }

        try {
            AccountManager data = new Gson().fromJson(Files.readString(Constants.getDataDir().resolve(STORE_FILE)), AccountManager.class);
            this.profiles = data.profiles;
            this.activeProfile = data.activeProfile;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Save profiles to json structure
    public void saveProfiles() {
        try {
            Files.writeString(Constants.getDataDir().resolve(STORE_FILE), new Gson().toJson(this, AccountManager.class));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Set<AccountProfile> getProfiles() {
        return profiles;
    }

    public UUID getActiveProfileRaw() {
        return activeProfile;
    }

    @Nullable
    public AccountProfile getActiveProfile() {
        return this.profiles.stream().filter(p -> p.uuid.equals(this.activeProfile))
                .findFirst()
                .orElse(null);
    }
}
