package dev.ftb.app.accounts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.ftb.app.AppPaths;
import dev.ftb.app.services.AppStorage;
import dev.ftb.app.services.storage.VaultException;
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
            AccountStore::empty,
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

                obj.addProperty("version", SPEC_VERSION);
            }
        }
        
        return jsonElement;
    }

    /// Removes the profile and its tokens.
    public void removeProfile(UUID uuid) {
        removeProfileData(uuid);

        try {
            var vault = appStorage.vault();
            for (MicrosoftProfile.TokenKey key : MicrosoftProfile.TokenKey.values()) {
                vault.remove(tokenKey(uuid, key));
            }
        } catch (VaultException e) {
            LOGGER.error("Failed to remove tokens for profile {} from the vault", uuid, e);
        }
    }

    /// Removes only the stored profile, leaving its tokens in the vault.
    private void removeProfileData(UUID uuid) {
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
        // Remove the profile if it already exists. Its tokens have already been replaced, so they stay.
        this.removeProfileData(profile.getUuid());

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

        for (MicrosoftProfile profile : store.get().profiles) {
            ProfileTokens tokens = legacyTokens.get(profile.getUuid());
            if (tokens != null) {
                try {
                    storeTokens(profile.getUuid(), tokens.toMap());
                } catch (VaultException e) {
                    LOGGER.error("Failed to migrate legacy tokens for profile {}", profile.getUuid(), e);

                    // The tokens are dropped from the file either way, so keep the profile and have the user log in again.
                    profile.markNotLoggedIn();
                }
            }
        }

        legacyTokens.clear();
        try {
            store.save();
            store.save(); // The first save backs up the old file, tokens included. The second replaces that backup.
        } catch (IOException ex) {
            LOGGER.error("Failed to save accounts after migrating tokens.", ex);
        }
    }
    
    /// @return the token, or `null` if it's missing or the vault can't be read
    @Nullable
    public String token(UUID uuid, MicrosoftProfile.TokenKey key) {
        try {
            return appStorage.vault().get(tokenKey(uuid, key));
        } catch (VaultException e) {
            LOGGER.error("Failed to read {} for profile {} from the vault", key.getKeyName(), uuid, e);
            return null;
        }
    }

    /// Stores the tokens in a single vault write, so either all of them are saved or none are.
    public void storeTokens(UUID uuid, Map<MicrosoftProfile.TokenKey, String> tokens) throws VaultException {
        Map<String, String> entries = new HashMap<>();
        tokens.forEach((key, value) -> entries.put(tokenKey(uuid, key), value));
        appStorage.vault().putAll(entries);
    }

    private static String tokenKey(UUID uuid, MicrosoftProfile.TokenKey key) {
        return "msa." + uuid + "." + key.getKeyName();
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
    private record ProfileTokens(@Nullable String minecraftAccessToken, @Nullable String microsoftAccessToken,
                                @Nullable String microsoftRefreshToken, @Nullable String xstsToken) {
        /// Skips any token missing from the old file, the refresh will then fail and ask the user to log in again.
        Map<MicrosoftProfile.TokenKey, String> toMap() {
            Map<MicrosoftProfile.TokenKey, String> map = new EnumMap<>(MicrosoftProfile.TokenKey.class);
            if (minecraftAccessToken != null) map.put(MicrosoftProfile.TokenKey.MINECRAFT_ACCESS_TOKEN, minecraftAccessToken);
            if (microsoftAccessToken != null) map.put(MicrosoftProfile.TokenKey.MICROSOFT_ACCESS_TOKEN, microsoftAccessToken);
            if (microsoftRefreshToken != null) map.put(MicrosoftProfile.TokenKey.MICROSOFT_REFRESH_TOKEN, microsoftRefreshToken);
            if (xstsToken != null) map.put(MicrosoftProfile.TokenKey.XSTS_TOKEN, xstsToken);
            return map;
        }
    }
}
