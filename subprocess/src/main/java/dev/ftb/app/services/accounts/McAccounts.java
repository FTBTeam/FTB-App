package dev.ftb.app.services.accounts;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.ftb.app.services.AppHttp;
import dev.ftb.app.services.accounts.minecraft.MicrosoftOAuthProcess;
import dev.ftb.app.services.accounts.minecraft.MicrosoftProfile;
import dev.ftb.app.services.accounts.minecraft.MicrosoftTokenExchange;
import dev.ftb.app.services.storage.Vault;
import dev.ftb.app.services.storage.VaultException;
import dev.ftb.app.structs.accounts.CodedError;
import dev.ftb.app.structs.accounts.ErrorWithCode;
import dev.ftb.app.structs.accounts.MicrosoftFinalToken;
import dev.ftb.app.structs.accounts.MinecraftProfileWithAuthData;
import dev.ftb.app.util.MiscUtils;
import dev.ftb.app.util.Result;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/// The signed in Minecraft (Microsoft) accounts, keyed by Minecraft profile UUID.
public class McAccounts extends MultiAccountStore<MicrosoftProfile, UUID, MicrosoftProfile.TokenKey> {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final String SPEC_VERSION = "1.0.2";

    /// The game keeps using the token to join servers while it runs, so make sure a launch has a good while left on it
    private static final long LIFE_MARGIN = Duration.ofHours(4).toSeconds();

    /// Errors from the Xbox and Minecraft steps that the user has to sort out. Anything else is treated as temporary.
    private static final Set<String> SIGN_IN_ERRORS = Set.of(
        CodedError.XBOX_ACCOUNT_MISSING.code,
        CodedError.XBOX_INVALID_REGION.code,
        CodedError.XBOX_ADULT_VERIFICATION.code,
        CodedError.XBOX_UNDER_18.code,
        CodedError.XBOX_BANNED.code,
        CodedError.XBOX_CHILD_RESTRICTED.code,
        CodedError.XBOX_TERMS_NOT_ACCEPTED.code,
        CodedError.XBOX_UNKNOWN_FAILURE_CODE.code,
        CodedError.MISSING_ENTITLEMENTS.code
    );

    private static final List<String> LEGACY_TOKEN_FIELDS = List.of(
        "minecraftAccessToken",
        "microsoftAccessToken",
        "microsoftRefreshToken",
        "xstsToken"
    );

    /// Tokens 1.0.1 kept in the vault that are no longer stored
    private static final List<String> DROPPED_TOKEN_KEYS = List.of("microsoftAccessToken", "xstsToken");

    /// Used by the migrator. Should be removed at some point.
    private final Map<UUID, ProfileTokens> legacyTokens = new HashMap<>();

    /// Used by the migrator. Should be removed at some point
    private final List<UUID> droppedTokenProfiles = new ArrayList<>();

    private final AppHttp http;
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
        this.http = appHttp;
        this.tokenExchange = new MicrosoftTokenExchange(appHttp);
    }

    @Override
    protected void onLoad() {
        this.migrateLegacyTokens();
        this.removeDroppedTokens();
    }

    /// Accepts the UUID with or without dashes, as Minecraft hands them out without.
    @Nullable
    public MicrosoftProfile getProfileFromUuid(String uuid) {
        return this.get(MiscUtils.createUuidFromStringWithoutDashes(uuid));
    }

    public MicrosoftTokenExchange tokenExchange() {
        return tokenExchange;
    }

    /// A Minecraft access token for the account with enough time left to play on, refreshing it first if needed.
    public LoginResult login(UUID id) {
        LoginResult current = this.currentIfFresh(id);
        if (current != null) {
            return current;
        }

        synchronized (this.refreshLock(id)) {
            // Another caller may have refreshed while this one waited
            current = this.currentIfFresh(id);
            if (current != null) {
                return current;
            }

            return this.refresh(id);
        }
    }

    /// @return `null` if the account needs a refresh
    private @Nullable LoginResult currentIfFresh(UUID id) {
        MicrosoftProfile profile = this.get(id);
        if (profile == null || profile.isNotLoggedIn()) {
            return new LoginResult.SignInRequired("This Minecraft account needs to sign in again.");
        }

        if (this.now() >= profile.getMinecraftAccessExpiresAt() - LIFE_MARGIN) {
            return null;
        }

        String token = this.token(id, MicrosoftProfile.TokenKey.MINECRAFT_ACCESS_TOKEN);
        return token == null ? null : new LoginResult.Ready(profile, token);
    }
    
    private LoginResult refresh(UUID id) {
        String refreshToken;
        try {
            refreshToken = this.readToken(id, MicrosoftProfile.TokenKey.MICROSOFT_REFRESH_TOKEN);
        } catch (VaultException e) {
            // Could be a locked keychain, so don't sign the account out over it
            LOGGER.error("Couldn't read the refresh token for Minecraft account {}", id, e);
            return new LoginResult.Unavailable("Couldn't read your Minecraft account from secure storage.");
        }

        if (refreshToken == null) {
            LOGGER.warn("Minecraft account {} has no refresh token, it has to sign in again", id);
            return this.signedOut(id, "This Minecraft account needs to sign in again.");
        }

        LOGGER.info("Refreshing Minecraft account {}", id);
        MicrosoftFinalToken microsoftToken;
        switch (this.tokenExchange.refresh(refreshToken)) {
            case TokenExchange.RefreshResult.Refreshed<MicrosoftFinalToken>(var token) -> microsoftToken = token;
            case TokenExchange.RefreshResult.Rejected<MicrosoftFinalToken>(var detail) -> {
                return this.signedOut(id, "Microsoft has signed this account out. Please sign in again.");
            }
            case TokenExchange.RefreshResult.Unavailable<MicrosoftFinalToken>(var reason) -> {
                LOGGER.warn("Couldn't refresh Minecraft account {} right now: {}", id, reason);
                return new LoginResult.Unavailable("Couldn't reach Microsoft to sign in. Check your connection and try again.");
            }
        }

        try {
            // Microsoft hands out a new refresh token each time, keep it even if a later step fails
            this.storeTokens(id, Map.of(MicrosoftProfile.TokenKey.MICROSOFT_REFRESH_TOKEN, microsoftToken.refreshToken()));
        } catch (VaultException e) {
            LOGGER.error("Couldn't save the new refresh token for Minecraft account {}", id, e);
        }

        Result<MinecraftProfileWithAuthData, ErrorWithCode> result = MicrosoftOAuthProcess.authWithMinecraft(this.http, microsoftToken.toHolder());
        if (result.isErr()) {
            ErrorWithCode error = result.unwrapErr();
            LOGGER.warn("Couldn't refresh Minecraft account {}: {} ({})", id, error.message(), error.code());
            return SIGN_IN_ERRORS.contains(error.code())
                ? this.signedOut(id, error.message())
                : new LoginResult.Unavailable(error.message());
        }

        MinecraftProfileWithAuthData data = result.unwrap();
        if (!id.equals(MiscUtils.createUuidFromStringWithoutDashes(data.profileData().id()))) {
            LOGGER.warn("Minecraft account {} refreshed into a different profile {}", id, data.profileData().id());
            return this.signedOut(id, "This Minecraft account needs to sign in again.");
        }

        try {
            this.storeTokens(id, Map.of(MicrosoftProfile.TokenKey.MINECRAFT_ACCESS_TOKEN, data.minecraftAccessToken()));
        } catch (VaultException e) {
            // Still usable for this launch. The old expiry is kept so the next launch tries again.
            LOGGER.error("Couldn't save the refreshed Minecraft token for account {}", id, e);
            MicrosoftProfile profile = Objects.requireNonNull(this.get(id));
            return new LoginResult.Ready(profile, data.minecraftAccessToken());
        }

        long expiresAt = this.now() + data.minecraftExpiresIn();
        this.update(id, p -> p.refreshed(
            data.profileData().name(),
            data.profileData().getActiveSkinUrl(),
            data.userHash(),
            expiresAt
        ));

        LOGGER.info("Refreshed Minecraft account {}", id);
        return new LoginResult.Ready(Objects.requireNonNull(this.get(id)), data.minecraftAccessToken());
    }

    private LoginResult signedOut(UUID id, String message) {
        this.update(id, MicrosoftProfile::markNotLoggedIn);
        this.notifySignedOut();
        return new LoginResult.SignInRequired(message);
    }

    private long now() {
        return Instant.now().getEpochSecond();
    }

    /// Older stores kept tokens in each profile. Pull them out before Gson sees them.
    @Override
    protected JsonObject runMigrator(JsonObject obj, String currentVersion) {
        if (currentVersion.equals("1.0.0") && obj.has("profiles") && obj.get("profiles").isJsonArray()) {
            var profiles = obj.get("profiles").getAsJsonArray();
            for (JsonElement element : profiles) {
                JsonObject profile = element.getAsJsonObject();
                if (!profile.has("minecraftAccessToken")) continue;

                legacyTokens.put(UUID.fromString(profile.get("uuid").getAsString()), new Gson().fromJson(profile, ProfileTokens.class));
                for (String field : LEGACY_TOKEN_FIELDS) {
                    profile.remove(field); // Purge the old tokens from the profile so they don't get saved again.
                }
            }
        }

        if (currentVersion.equals("1.0.1") && obj.has("profiles") && obj.get("profiles").isJsonArray()) {
            for (JsonElement element : obj.get("profiles").getAsJsonArray()) {
                JsonObject profile = element.getAsJsonObject();
                if (profile.has("uuid")) {
                    droppedTokenProfiles.add(UUID.fromString(profile.get("uuid").getAsString()));
                }
            }
        }

        obj.addProperty("version", SPEC_VERSION);
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

    private void removeDroppedTokens() {
        if (droppedTokenProfiles.isEmpty()) {
            return;
        }

        for (UUID id : droppedTokenProfiles) {
            for (String key : DROPPED_TOKEN_KEYS) {
                this.removeStaleToken(id, key);
            }
        }

        droppedTokenProfiles.clear();
        this.save(); // Writes the new version, so this only runs once
    }

    /// Only needed whilst migrations are in place, to convert old token storage to the vault.
    private record ProfileTokens(@Nullable String minecraftAccessToken, @Nullable String microsoftRefreshToken) {
        /// Skips any token missing from the old file, the refresh will then fail and ask the user to log in again.
        Map<MicrosoftProfile.TokenKey, String> toMap() {
            Map<MicrosoftProfile.TokenKey, String> map = new EnumMap<>(MicrosoftProfile.TokenKey.class);
            if (minecraftAccessToken != null) map.put(MicrosoftProfile.TokenKey.MINECRAFT_ACCESS_TOKEN, minecraftAccessToken);
            if (microsoftRefreshToken != null) map.put(MicrosoftProfile.TokenKey.MICROSOFT_REFRESH_TOKEN, microsoftRefreshToken);
            return map;
        }
    }

    public sealed interface LoginResult {
        /// `profile` is as of the refresh, so its name and xuid match the token
        record Ready(MicrosoftProfile profile, String accessToken) implements LoginResult {}

        /// The account has been marked as signed out
        record SignInRequired(String message) implements LoginResult {}

        /// Couldn't refresh right now, the account is still signed in
        record Unavailable(String message) implements LoginResult {}
    }
}
