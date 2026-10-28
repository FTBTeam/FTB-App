package dev.ftb.app.accounts;

import com.google.gson.JsonObject;
import dev.ftb.app.accounts.auth.MicrosoftOAuthProcess;
import dev.ftb.app.accounts.auth.MicrosoftRequests;
import dev.ftb.app.accounts.data.ErrorWithCode;
import dev.ftb.app.accounts.data.MinecraftProfileData;
import dev.ftb.app.services.AppHttp;
import dev.ftb.app.services.AppStorage;
import dev.ftb.app.services.storage.VaultException;
import dev.ftb.app.util.Result;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a logged in Microsoft account
 */
public class MicrosoftProfile {
    private static final Logger LOGGER = LoggerFactory.getLogger(MicrosoftProfile.class);
    
    private UUID uuid;
    private String minecraftName;
    
    private long minecraftAccessExpiresAt;
    private long microsoftAccessExpiresAt;
    
    private String xstsUserHash;
    private long xstsNotAfter;

    private String skinUrl;
    private boolean notLoggedIn = false;

    // This one is purely for serialization and should not be used for anything else
    public MicrosoftProfile() {
    }

    public MicrosoftProfile(UUID uuid, String minecraftName, int minecraftAccessExpiresIn, int microsoftAccessExpiresIn, String xstsUserHash, String xstsNotAfter, String skinUrl) {
        var now = Instant.now();
        
        this.uuid = uuid;
        this.minecraftName = minecraftName;
        this.minecraftAccessExpiresAt = now.plus(Duration.ofSeconds(minecraftAccessExpiresIn)).getEpochSecond();
        this.microsoftAccessExpiresAt = now.plus(Duration.ofSeconds(microsoftAccessExpiresIn)).getEpochSecond();
        this.xstsUserHash = xstsUserHash;
        this.xstsNotAfter = Instant.parse(xstsNotAfter).getEpochSecond();
        this.skinUrl = skinUrl;
    }
    
    @Nullable
    public static MicrosoftProfile create(AppStorage storage, UUID uuid, String minecraftName, String minecraftAccessToken, int minecraftAccessExpiresIn, String microsoftAccessToken, String microsoftRefreshToken, int microsoftAccessExpiresIn, String xstsUserHash, String xstsNotAfter, String xstsToken, String skinUrl) {
        try {
            var vault = storage.vault();
            vault.put(tokenKey(uuid, TokenKey.MINECRAFT_ACCESS_TOKEN), minecraftAccessToken);
            vault.put(tokenKey(uuid, TokenKey.MICROSOFT_ACCESS_TOKEN), microsoftAccessToken);
            vault.put(tokenKey(uuid, TokenKey.MICROSOFT_REFRESH_TOKEN), microsoftRefreshToken);
            vault.put(tokenKey(uuid, TokenKey.XSTS_TOKEN), xstsToken);
        } catch (VaultException e) {
            LOGGER.error("Failed to store tokens in the vault", e);
            return null;
        }

        // This only returns if the store is successful.
        return new MicrosoftProfile(uuid, minecraftName, minecraftAccessExpiresIn, microsoftAccessExpiresIn, xstsUserHash, xstsNotAfter, skinUrl);
    }
    
    /**
     * Checks to see if the account is still valid or if the tokens need to be refreshed
     */
    public ValidCheckResult isValid() {
        if (this.notLoggedIn) {
            return ValidCheckResult.NOT_LOGGED_IN;
        }
        
        var stillValid = Instant.now().getEpochSecond() < this.minecraftAccessExpiresAt;
        return stillValid ? ValidCheckResult.VALID : ValidCheckResult.EXPIRED;
    }
    
    /**
     * Refreshes the account if needed
     */
    public boolean refreshAccount(AppHttp http, AccountManager accounts, AppStorage storage, boolean forceRefresh) {
        try {
            LOGGER.info("Refreshing account for {}", this.minecraftName);
            boolean result = this.unsafeRefreshAccount(http, accounts, storage, forceRefresh);
            if (!result) {
                LOGGER.error("Failed to refresh account for {}", this.minecraftName);
                
                this.notLoggedIn = true;
                accounts.saveProfiles();
            }
            
            LOGGER.info("Finished refreshing account for {}", this.minecraftName);
            return result;
        } catch (Exception e) {
            LOGGER.error("Failed to refresh account", e);
            this.notLoggedIn = true;
            accounts.saveProfiles();
            return false;
        }
    }
    
    public boolean refreshAccount(AppHttp http, AccountManager accounts, AppStorage storage) {
        return this.refreshAccount(http, accounts, storage, false);
    }

    /**
     * Big thanks to ATLauncher for the rough implementation of this method
     * <a href="https://github.com/ATLauncher/ATLauncher/blob/master/src/main/java/com/atlauncher/data/MicrosoftAccount.java#L159">...</a>
     */
    private boolean unsafeRefreshAccount(AppHttp http, AccountManager accounts, AppStorage storage, boolean forceRefresh) {
        var now = Instant.now();
        
        if (forceRefresh || now.getEpochSecond() >= this.microsoftAccessExpiresAt) {
            LOGGER.info("Xbox token expired, refreshing");
            var microsoftAccessToken = getMicrosoftToken(storage);
            if (microsoftAccessToken == null) {
                LOGGER.error("Microsoft access token is missing, cannot refresh");
                return false;
            }
            
            var microsoftRefreshToken = getMicrosoftRefreshToken(storage);
            if (microsoftRefreshToken == null) {
                LOGGER.error("Microsoft refresh token is missing, cannot refresh");
                return false;
            }
            
            var refreshResult = MicrosoftRequests.refreshWithXbox(http, microsoftRefreshToken);
            if (refreshResult.isErr()) {
                LOGGER.error("Failed to refresh Xbox token: {}", refreshResult.unwrapErr());
                return false;
            }
            
            LOGGER.info("Successfully refreshed Xbox token");
            var jsonData = refreshResult.unwrap();
            
            try {
                storage.vault().put(tokenKey(uuid, TokenKey.MICROSOFT_ACCESS_TOKEN), jsonData.get("access_token").getAsString());
                storage.vault().put(tokenKey(uuid, TokenKey.MICROSOFT_REFRESH_TOKEN), jsonData.get("refresh_token").getAsString());
            } catch (VaultException e) {
                LOGGER.error("Failed to store refreshed tokens in the vault", e);
                return false;
            }
            
            this.microsoftAccessExpiresAt = now.plus(Duration.ofSeconds(jsonData.get("expires_in").getAsLong())).getEpochSecond();
            accounts.saveProfiles();
        }
        
        if (forceRefresh || now.getEpochSecond() >= this.xstsNotAfter) {
            LOGGER.info("XSTS token expired, refreshing");
            var microsoftAccessToken = getMicrosoftToken(storage);
            if (microsoftAccessToken == null) {
                LOGGER.error("Microsoft access token is missing, cannot refresh XSTS token");
                return false;
            }
            
            var xstsResult = MicrosoftRequests.authenticateWithXbox(http, microsoftAccessToken);
            if (xstsResult.isErr()) {
                LOGGER.error("Failed to authenticate with XSTS: {}", xstsResult.unwrapErr());
                return false;
            }
            

            JsonObject xstsData = xstsResult.unwrap();
            if (!xstsData.has("DisplayClaims") || !xstsData.has("Token")) {
                LOGGER.error("Invalid XSTS response, missing expected fields. Present fields: {}", xstsData.keySet());
                return false;
            }
            
            var xstsTokenResult = MicrosoftRequests.authenticateWithXSTS(http, xstsData.get("Token").getAsString());
            if (xstsTokenResult.isErr()) {
                LOGGER.error("Failed to authenticate with XSTS Tokens: {}", xstsTokenResult.unwrapErr());
                return false;
            }

            JsonObject xstsTokenData = xstsTokenResult.unwrap();
            if (!xstsTokenData.has("DisplayClaims")) {
                LOGGER.error("Invalid XSTS Tokens response, missing expected fields. Present fields: {}", xstsTokenData.keySet());
                return false;
            }
            
            Result<String, ErrorWithCode> displayClaims = MicrosoftOAuthProcess.getUserHashFromDisplayClaims(xstsTokenData.get("DisplayClaims").getAsJsonObject());
            if (displayClaims.isErr()) {
                LOGGER.error("Failed to get user hash from display claims: {}", displayClaims.unwrapErr());
                return false;
            }
            
            LOGGER.info("Successfully refreshed XSTS token");
            try {
                storage.vault().put(tokenKey(uuid, TokenKey.XSTS_TOKEN), xstsTokenData.get("Token").getAsString());
            } catch (VaultException e) {
                LOGGER.error("Failed to store refreshed XSTS token in the vault", e);
                return false;
            }
            
            this.xstsUserHash = displayClaims.unwrap();
            this.xstsNotAfter = Instant.parse(xstsTokenData.get("NotAfter").getAsString()).getEpochSecond();
            accounts.saveProfiles();
        }
        
        if (forceRefresh || now.getEpochSecond() >= this.minecraftAccessExpiresAt) {
            LOGGER.info("Minecraft token expired, refreshing");
            var xstsToken = getXstsToken(storage);
            if (xstsToken == null) {
                LOGGER.error("XSTS token is missing, cannot refresh Minecraft token");
                return false;
            }
            
            var minecraftResult = MicrosoftRequests.authenticateWithMinecraft(http, xstsToken, this.xstsUserHash);
            if (minecraftResult.isErr()) {
                LOGGER.error("Failed to authenticate with Minecraft: {}", minecraftResult.unwrapErr());
                return false;
            }

            JsonObject minecraftData = minecraftResult.unwrap();
            String accessToken = minecraftData.get("access_token").getAsString();
            try {
                storage.vault().put(tokenKey(uuid, TokenKey.MINECRAFT_ACCESS_TOKEN), accessToken);
            } catch (VaultException e) {
                LOGGER.error("Failed to store refreshed Minecraft token in the vault", e);
                return false;
            }
            
            this.minecraftAccessExpiresAt = now.plus(Duration.ofSeconds(minecraftData.get("expires_in").getAsLong())).getEpochSecond();
            
            // Now rerun the entitlements check and profile fetching
            var profile = MicrosoftOAuthProcess.checkEntitlementsAndGetProfile(http, accessToken);
            if (profile.isErr()) {
                LOGGER.error("Failed to check entitlements and get profile: {}", profile.unwrapErr());
                return false;
            }

            LOGGER.info("Successfully refreshed Minecraft token");
            MinecraftProfileData profileData = profile.unwrap();
            
            this.minecraftName = profileData.name();
            this.skinUrl = profileData.getActiveSkinUrl();
        }

        this.notLoggedIn = false;
        accounts.saveProfiles();
        
        return true;
    }

    @Nullable
    public String getMinecraftToken(AppStorage storage) {
        try {
            return storage.vault().get(tokenKey(uuid, TokenKey.MINECRAFT_ACCESS_TOKEN));
        } catch (VaultException e) {
            LOGGER.error("Failed to retrieve Minecraft token from vault", e);
            return null;
        }
    }
    
    @Nullable
    private String getMicrosoftToken(AppStorage storage) {
        try {
            return storage.vault().get(tokenKey(uuid, TokenKey.MICROSOFT_ACCESS_TOKEN));
        } catch (VaultException e) {
            LOGGER.error("Failed to retrieve Microsoft token from vault", e);
            return null;
        }
    }
    
    @Nullable
    private String getMicrosoftRefreshToken(AppStorage storage) {
        try {
            return storage.vault().get(tokenKey(uuid, TokenKey.MICROSOFT_REFRESH_TOKEN));
        } catch (VaultException e) {
            LOGGER.error("Failed to retrieve Microsoft refresh token from vault", e);
            return null;
        }
    }
    
    @Nullable
    public String getXstsToken(AppStorage storage) {
        try {
            return storage.vault().get(tokenKey(uuid, TokenKey.XSTS_TOKEN));
        } catch (VaultException e) {
            LOGGER.error("Failed to retrieve XSTS token from vault", e);
            return null;
        }
    }
    
    public UUID getUuid() {
        return uuid;
    }

    public String getMinecraftName() {
        return minecraftName;
    }

    public String getSkinUrl() {
        return skinUrl;
    }
    
    public String getXstsUserHash() {
        return xstsUserHash;
    }
    
    public SharableData toSharableData() {
        return new SharableData(this.uuid, this.minecraftName, this.skinUrl);
    }
    
    public record SharableData(UUID uuid, String minecraftName, String skinUrl) { }
    
    public enum ValidCheckResult {
        VALID,
        EXPIRED,
        NOT_LOGGED_IN,
        TOTAL_FAILURE
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MicrosoftProfile that = (MicrosoftProfile) o;
        return Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uuid);
    }
    
    // TODO: Close scope access once the migrator is removed. 
    public static String tokenKey(UUID uuid, TokenKey key) {
        return uuid + "." + key.getKeyName();
    }
    
    public enum TokenKey {
        MINECRAFT_ACCESS_TOKEN("minecraftAccessToken"),
        MICROSOFT_ACCESS_TOKEN("microsoftAccessToken"),
        MICROSOFT_REFRESH_TOKEN("microsoftRefreshToken"),
        XSTS_TOKEN("xstsToken");
        
        private final String keyName;
        
        TokenKey(String keyName) {
            this.keyName = keyName;
        }
        
        public String getKeyName() {
            return keyName;
        }
    }
}
