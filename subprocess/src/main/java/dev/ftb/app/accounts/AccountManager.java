package dev.ftb.app.accounts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.covers1624.quack.gson.JsonUtils;
import dev.ftb.app.Constants;
import dev.ftb.app.util.MiscUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class AccountManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = LogManager.getLogger();

    public static final String SPEC_VERSION = "1.0.0";

    private static final Path STORE_FILE = Constants.STORAGE_DIR.resolve("mc-accounts.json");
    private static final Path OLD_STORE_FILE = Constants.getDataDir().resolve("profiles.json");
    
    private static final AccountManager ACCOUNT_MANAGER = new AccountManager();

    private Set<MicrosoftProfile> profiles = new HashSet<>();

    @Nullable
    private MicrosoftProfile activeProfile = null;

    private AccountManager() {
        loadProfiles();
    }

    public static AccountManager get() {
        return ACCOUNT_MANAGER;
    }

    public boolean removeProfile(UUID uuid) {
        boolean removed = this.profiles.stream().filter(p -> p.getUuid().equals(uuid)).findFirst()
                .map(e -> this.profiles.remove(e))
                .orElse(false);

        if (this.activeProfile != null && this.activeProfile.getUuid().equals(uuid)) {
            this.activeProfile = this.profiles.stream().findFirst().orElse(null);
        }

        if (removed) {
            this.saveProfiles();
        }

        return removed;
    }

    public void setActiveProfile(UUID uuid) {
        this.activeProfile = this.profiles.stream().filter(p -> p.getUuid().equals(uuid)).findFirst().orElse(null);
        this.saveProfiles();
    }

    public void addProfile(MicrosoftProfile profile) {
        // Remove the profile if it already exists.
        this.removeProfile(profile.getUuid());

        // Now add the profile
        this.profiles.add(profile);
        this.activeProfile = profile;
        this.saveProfiles();
    }

    // Load profiles from json structure
    public void loadProfiles() {
        if (Files.exists(OLD_STORE_FILE)) {
            try {
                Files.deleteIfExists(OLD_STORE_FILE);
            } catch (IOException e) {
                LOGGER.error("Failed to move old profiles file.", e);
            }
        }
        
        if (!Files.exists(STORE_FILE)) {
            return;
        }

        try {
            // Get the profile data from the json file.
            AccountStore store = JsonUtils.parse(GSON, STORE_FILE, AccountStore.class);
            if (store == null) {
                return;
            }
            
            // TODO: create a migration system if this ever gets changed again.
            //       For this implementation, we actually want it to drop the old data as it's likely fucked anyway.
            if (!Objects.equals(store.version, SPEC_VERSION)) {
                // Allow the system to create a new one
                return;
            }

            this.profiles = store.profiles;
            this.activeProfile = store.profiles.stream()
                    .filter(e -> e.getUuid().equals(store.activeProfile))
                    .findFirst()
                    .orElse(null);
        } catch (IOException | JsonSyntaxException e) {
            LOGGER.error("Failed to read Profiles.", e);
        }
    }

    // Save profiles to json structure
    public void saveProfiles() {
        try {
            AccountStore store = new AccountStore(profiles, activeProfile != null ? activeProfile.getUuid() : null);
            JsonUtils.write(GSON, STORE_FILE, store);
        } catch (IOException e) {
            LOGGER.error("Failed to write profiles.", e);
        }
    }

    public Set<MicrosoftProfile> getProfiles() {
        return profiles;
    }

    @Nullable
    public MicrosoftProfile getProfileFromUuid(String uuid) {
        return profiles.stream()
            .filter(p -> p.getUuid().equals(MiscUtils.createUuidFromStringWithoutDashes(uuid)))
            .findFirst()
            .orElse(null);
    }

    @Nullable
    public MicrosoftProfile getActiveProfile() {
        return this.activeProfile;
    }
}
