package dev.ftb.app.accounts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.ftb.app.AppPaths;
import dev.ftb.app.util.JsonStore;
import dev.ftb.app.util.MiscUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

public class AccountManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = LogManager.getLogger();

    public static final String SPEC_VERSION = "1.0.0";

    private final Path oldStoreFile;
    
    private final JsonStore<AccountStore> store;
    
    @Nullable
    private MicrosoftProfile activeProfile = null;

    public AccountManager(AppPaths appPaths) {
        this.oldStoreFile = appPaths.workingDir().resolve("profiles.json");
        
        this.store = new JsonStore<>(
            appPaths.storageDir().resolve("mc-accounts.json"),
            GSON,
            AccountStore.class,
            () -> AccountStore.EMPTY
        );
        
        loadProfiles();
    }
    
    public void removeProfile(UUID uuid) {
        try {
            this.store.update(existing -> {
                existing.profiles.removeIf(p -> p.getUuid().equals(uuid));
                
                if (existing.activeProfile != null && existing.activeProfile.equals(uuid)) {
                    existing.activeProfile = existing.profiles.stream().findFirst().map(MicrosoftProfile::getUuid).orElse(null);
                }
            });
        } catch (IOException e) {
            LOGGER.error("Failed to remove profile", e);
        }
    }

    public void setActiveProfile(UUID uuid) {
        try {
            this.store.update(existing -> {
                // Update but ensure the profile exists first, otherwise set to null
                existing.activeProfile = existing.profiles.stream().filter(p -> p.getUuid().equals(uuid)).findFirst()
                    .map(MicrosoftProfile::getUuid)
                    .orElse(null);
            });
        } catch (IOException e) {
            LOGGER.error("Failed to set active profile", e);
        }
    }

    public void addProfile(MicrosoftProfile profile) {
        // Remove the profile if it already exists.
        this.removeProfile(profile.getUuid());

        try {
            this.store.update(existing -> {
                existing.profiles.add(profile);
                existing.activeProfile = profile.getUuid();
            });
        } catch (IOException e) {
            LOGGER.error("Failed to add profile", e);
        }
    }

    /// Load profiles from json structure
    public void loadProfiles() {
        this.cleanupOldFile();
        try {
            this.store.load();
            this.activeProfile = store.get().profiles
                    .stream()
                    .filter(e -> e.getUuid().equals(store.get().activeProfile))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            LOGGER.error("Failed to load profiles.", e);
        }
    }

    private void cleanupOldFile() {
        // Just drop the old data it's not worth trying to migrate it.
        if (Files.exists(oldStoreFile)) {
            try {
                Files.deleteIfExists(oldStoreFile);
            } catch (IOException e) {
                LOGGER.error("Failed to move old profiles file.", e);
            }
        }
    }

    // Save profiles to json structure
    public void saveProfiles() {
        try {
            this.store.save();
        } catch (IOException e) {
            LOGGER.error("Failed to save profiles.", e);
        }
    }

    public Set<MicrosoftProfile> getProfiles() {
        return this.store.get().profiles;
    }

    @Nullable
    public MicrosoftProfile getProfileFromUuid(String uuid) {
        return this.store.get().profiles.stream()
            .filter(p -> p.getUuid().equals(MiscUtils.createUuidFromStringWithoutDashes(uuid)))
            .findFirst()
            .orElse(null);
    }

    @Nullable
    public MicrosoftProfile getActiveProfile() {
        return this.activeProfile;
    }
}
