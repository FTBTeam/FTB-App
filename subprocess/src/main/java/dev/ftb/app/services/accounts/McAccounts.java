package dev.ftb.app.services.accounts;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.ftb.app.services.AppHttp;
import dev.ftb.app.services.accounts.minecraft.MicrosoftProfile;
import dev.ftb.app.services.accounts.minecraft.MicrosoftTokenExchange;
import dev.ftb.app.services.storage.Vault;
import dev.ftb.app.services.storage.VaultException;
import dev.ftb.app.util.MiscUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;

/// The signed in Minecraft (Microsoft) accounts, keyed by Minecraft profile UUID.
public class McAccounts extends MultiAccountStore<MicrosoftProfile, UUID, MicrosoftProfile.TokenKey> {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final String SPEC_VERSION = "1.0.1";

    private static final List<String> LEGACY_TOKEN_FIELDS = List.of(
        "minecraftAccessToken",
        "microsoftAccessToken",
        "microsoftRefreshToken",
        "xstsToken"
    );

    /// Filled by [#runMigrator] during load, moved to the vault by [#onLoad()].
    private final Map<UUID, ProfileTokens> legacyTokens = new HashMap<>();

    private final MicrosoftTokenExchange tokenExchange;

    public McAccounts(Path storageDir, Vault vault, AppHttp appHttp) {
        super(
            storageDir,
            vault,
            "mc-accounts",
            SPEC_VERSION,
            "msa",
            MicrosoftProfile.class,
            UUID.class,
            MicrosoftProfile.TokenKey.class,
            MicrosoftProfile::getUuid
        );
        this.tokenExchange = new MicrosoftTokenExchange(appHttp);
    }

    @Override
    protected void onLoad() {
        this.migrateLegacyTokens();
    }

    /// Accepts the UUID with or without dashes, as Minecraft hands them out without.
    @Nullable
    public MicrosoftProfile getProfileFromUuid(String uuid) {
        return this.get(MiscUtils.createUuidFromStringWithoutDashes(uuid));
    }

    public MicrosoftTokenExchange tokenExchange() {
        return tokenExchange;
    }

    /// Older stores kept tokens in each profile. Pull them out before Gson sees them.
    @Override
    protected JsonObject runMigrator(JsonObject obj, String currentVersion) {
        if (currentVersion.equals("1.0.0")) {
            if (!obj.has("profiles") || !obj.get("profiles").isJsonArray()) {
                return obj;
            }

            var profiles = obj.get("profiles").getAsJsonArray();
            for (JsonElement element : profiles) {
                JsonObject profile = element.getAsJsonObject();
                if (!profile.has("minecraftAccessToken")) continue;

                legacyTokens.put(UUID.fromString(profile.get("uuid").getAsString()), new Gson().fromJson(profile, ProfileTokens.class));
                for (String field : LEGACY_TOKEN_FIELDS) {
                    profile.remove(field); // Purge the old tokens from the profile so they don't get saved again.
                }
            }

            obj.addProperty("version", SPEC_VERSION);
        }

        return obj;
    }

    private void migrateLegacyTokens() {
        if (legacyTokens.isEmpty()) {
            return;
        }

        for (MicrosoftProfile profile : this.all()) {
            ProfileTokens tokens = legacyTokens.get(profile.getUuid());
            if (tokens != null) {
                try {
                    storeTokens(profile.getUuid(), tokens.toMap());
                } catch (VaultException e) {
                    LOGGER.error("Failed to migrate legacy tokens for profile {}", profile.getUuid(), e);

                    // The tokens are dropped from the file either way, so keep the profile and have the user log in again.
                    this.update(profile.getUuid(), MicrosoftProfile::markNotLoggedIn);
                }
            }
        }

        legacyTokens.clear();
        this.save();
        this.save(); // The first save backs up the old file, tokens included. The second replaces that backup.
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
