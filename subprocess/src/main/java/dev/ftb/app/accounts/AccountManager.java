package dev.ftb.app.accounts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.ftb.app.AppPaths;
import dev.ftb.app.services.AppStorage;
import dev.ftb.app.util.JsonStore;
import dev.ftb.app.util.MiscUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.*;

public class AccountManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = LogManager.getLogger();

    public static final String SPEC_VERSION = "1.0.1";
    
    private static final List<String> LEGACY_TOKEN_FIELDS = List.of(
        "minecraftAccessToken",
        "microsoftAccessToken",
        "microsoftRefreshToken",
        "xstsToken"
    );
    private final Map<UUID, ProfileTokens> legacyTokens = new HashMap<>();
    
    private final JsonStore<AccountStore> store;
    
    @Nullable
    private MicrosoftProfile activeProfile = null;
    
    private final AppStorage appStorage;

    public AccountManager(AppPaths appPaths, AppStorage storage) {
        this.appStorage = storage;
        
        this.store = new JsonStore<>(
            appPaths.storageDir().resolve("mc-accounts.json"),
            GSON,
            AccountStore.class,
            () -> AccountStore.EMPTY,
            this::migrator
        );
        
        loadProfiles();
    }

    /// Older stores kept tokens in each profile. Pull them out before Gson sees them.
    private JsonElement migrator(JsonElement jsonElement) {
        // this shouldn't happen but we'll support it anyway
        if (!jsonElement.isJsonObject()) return jsonElement;
        
        var obj = jsonElement.getAsJsonObject();
        if (obj.has("version")) {
            if (obj.get("version").getAsString().equals(SPEC_VERSION)) {
                return jsonElement;
            }
            
            var version = obj.get("version").getAsString();
            // 1.0.0 -> 1.0.1 migration: Move tokens to the vault
            if (version.equals("1.0.0")) {
                if (!obj.has("profiles") || !obj.get("profiles").isJsonArray()) return jsonElement;
                
                var profiles = obj.get("profiles").getAsJsonArray();
                for (JsonElement element : profiles) {
                    JsonObject profile = element.getAsJsonObject();
                    if (!profile.has("minecraftAccessToken")) continue;

                    legacyTokens.put(UUID.fromString(profile.get("uuid").getAsString()), GSON.fromJson(profile, ProfileTokens.class));
                    for (String field : LEGACY_TOKEN_FIELDS) {
                        profile.remove(field); // Purge the old tokens from the profile so they don't get saved again.
                    }
                }
            }
        }
        
        return jsonElement;
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
        Optional<MicrosoftProfile> foundActiveProfile = this.store.get().profiles.stream().filter(p -> p.getUuid().equals(uuid)).findFirst();
        
        try {
            this.store.update(existing -> {
                // Update but ensure the profile exists first, otherwise set to null
                existing.activeProfile = foundActiveProfile.map(MicrosoftProfile::getUuid).orElse(null);
            });
            
            this.activeProfile = foundActiveProfile.orElse(null);
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
        try {
            this.store.load();
            this.migrateLegacyTokens();
            this.activeProfile = store.get().profiles
                    .stream()
                    .filter(e -> e.getUuid().equals(store.get().activeProfile))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            LOGGER.error("Failed to load profiles.", e);
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

    private void migrateLegacyTokens() {
        if (legacyTokens.isEmpty()) {
            return;
        }

        List<UUID> failedProfiles = new ArrayList<>();
        for (MicrosoftProfile profile : store.get().profiles) {
            ProfileTokens tokens = legacyTokens.get(profile.getUuid());
            if (tokens != null) {
                try {
                    var vault = appStorage.vault();
                    vault.put(MicrosoftProfile.tokenKey(profile.getUuid(), MicrosoftProfile.TokenKey.MINECRAFT_ACCESS_TOKEN), tokens.minecraftAccessToken);
                    vault.put(MicrosoftProfile.tokenKey(profile.getUuid(), MicrosoftProfile.TokenKey.MICROSOFT_ACCESS_TOKEN), tokens.microsoftAccessToken);
                    vault.put(MicrosoftProfile.tokenKey(profile.getUuid(), MicrosoftProfile.TokenKey.MICROSOFT_REFRESH_TOKEN), tokens.microsoftRefreshToken);
                    vault.put(MicrosoftProfile.tokenKey(profile.getUuid(), MicrosoftProfile.TokenKey.XSTS_TOKEN), tokens.xstsToken);
                } catch (Exception e) {
                    LOGGER.error("Failed to migrate legacy tokens for profile {}", profile.getUuid(), e);
                    
                    // If it fails we basically have no choice but to remove the profile, as it will be in a broken state.
                    failedProfiles.add(profile.getUuid());
                }
            }
        }
        
        // This has to be done outside the loop to avoid concurrent modification exceptions.
        for (UUID failedProfile : failedProfiles) {
            this.removeProfile(failedProfile);
        }

        legacyTokens.clear();
        try {
            store.save();
            store.save(); // The first save backs up the old file, tokens included. The second replaces that backup.
        } catch (IOException ex) {
            LOGGER.error("Failed to save accounts after migrating tokens.", ex);
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
    
    /// Only needed whilst migrations are in place, to convert old token storage to the vault.
    private record ProfileTokens(String minecraftAccessToken, String microsoftAccessToken,
                                String microsoftRefreshToken, String xstsToken) { }
}
