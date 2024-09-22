package net.creeperhost.creeperlauncher.accounts;

import com.google.gson.JsonObject;
import net.creeperhost.creeperlauncher.accounts.auth.MicrosoftOAuthProcess;
import net.creeperhost.creeperlauncher.accounts.auth.MicrosoftRequests;
import net.creeperhost.creeperlauncher.accounts.data.ErrorWithCode;
import net.creeperhost.creeperlauncher.accounts.data.MinecraftProfileData;
import net.creeperhost.creeperlauncher.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a logged in Microsoft account
 */
public class MicrosoftProfile {
    private static final Logger LOGGER = LoggerFactory.getLogger(MicrosoftProfile.class);
    
    private UUID uuid;
    private String minecraftName;
    
    private String minecraftAccessToken;
    private long minecraftAccessExpiresAt;
    
    private String microsoftAccessToken;
    private String microsoftRefreshToken;
    private long microsoftAccessExpiresAt;
    
    private String xstsUserHash;
    private String xstsToken;
    private long xstsNotAfter;

    private String skinUrl;
    private boolean notLoggedIn = false;

    public MicrosoftProfile(UUID uuid, String minecraftName, String minecraftAccessToken, int minecraftAccessExpiresIn, String microsoftAccessToken, String microsoftRefreshToken, int microsoftAccessExpiresIn, String xstsUserHash, String xstsNotAfter, String xstsToken, String skinUrl) {
        var now = Instant.now();
        
        this.uuid = uuid;
        this.minecraftName = minecraftName;
        this.minecraftAccessToken = minecraftAccessToken;
        this.minecraftAccessExpiresAt = now.plus(Duration.ofSeconds(minecraftAccessExpiresIn)).getEpochSecond();
        this.microsoftAccessToken = microsoftAccessToken;
        this.microsoftRefreshToken = microsoftRefreshToken;
        this.microsoftAccessExpiresAt = now.plus(Duration.ofSeconds(microsoftAccessExpiresIn)).getEpochSecond();
        this.xstsUserHash = xstsUserHash;
        this.xstsNotAfter = Instant.parse(xstsNotAfter).getEpochSecond();
        this.xstsToken = xstsToken;
        this.skinUrl = skinUrl;
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
    public boolean refreshAccount(boolean forceRefresh) {
        try {
            LOGGER.info("Refreshing account for {}", this.minecraftName);
            boolean result = this.unsafeRefreshAccount(forceRefresh);
            if (!result) {
                LOGGER.error("Failed to refresh account for {}", this.minecraftName);
                
                this.notLoggedIn = true;
                AccountManager.get().saveProfiles();
            }
            
            LOGGER.info("Finished refreshing account for {}", this.minecraftName);
            return result;
        } catch (Exception e) {
            LOGGER.error("Failed to refresh account", e);
            this.notLoggedIn = true;
            AccountManager.get().saveProfiles();
            return false;
        }
    }
    
    public boolean refreshAccount() {
        return this.refreshAccount(false);
    }

    /**
     * Big thanks to ATLauncher for the rough implementation of this method
     * <a href="https://github.com/ATLauncher/ATLauncher/blob/master/src/main/java/com/atlauncher/data/MicrosoftAccount.java#L159">...</a>
     */
    private boolean unsafeRefreshAccount(boolean forceRefresh) {
        var now = Instant.now();
        
        if (forceRefresh || now.getEpochSecond() >= this.microsoftAccessExpiresAt) {
            LOGGER.info("Xbox token expired, refreshing");
            var refreshResult = MicrosoftRequests.refreshWithXbox(this.microsoftRefreshToken);
            if (refreshResult.isErr()) {
                LOGGER.error("Failed to refresh Xbox token: {}", refreshResult.unwrapErr());
                return false;
            }
            
            LOGGER.info("Successfully refreshed Xbox token");
            var jsonData = refreshResult.unwrap();
            this.microsoftAccessToken = jsonData.get("access_token").getAsString();
            this.microsoftRefreshToken = jsonData.get("refresh_token").getAsString();
            this.microsoftAccessExpiresAt = now.plus(Duration.ofSeconds(jsonData.get("expires_in").getAsLong())).getEpochSecond();
            AccountManager.get().saveProfiles();
        }
        
        if (forceRefresh || now.getEpochSecond() >= this.xstsNotAfter) {
            LOGGER.info("XSTS token expired, refreshing");
            var xstsResult = MicrosoftRequests.authenticateWithXbox(this.microsoftAccessToken);
            if (xstsResult.isErr()) {
                LOGGER.error("Failed to authenticate with XSTS: {}", xstsResult.unwrapErr());
                return false;
            }
            

            JsonObject xstsData = xstsResult.unwrap();
            if (!xstsData.has("DisplayClaims") || !xstsData.has("Token")) {
                LOGGER.error("Invalid XSTS response: {}", xstsData);
                return false;
            }
            
            var xstsTokenResult = MicrosoftRequests.authenticateWithXSTS(xstsData.get("Token").getAsString());
            if (xstsTokenResult.isErr()) {
                LOGGER.error("Failed to authenticate with XSTS Tokens: {}", xstsTokenResult.unwrapErr());
                return false;
            }

            JsonObject xstsTokenData = xstsTokenResult.unwrap();
            if (!xstsTokenData.has("DisplayClaims")) {
                LOGGER.error("Invalid XSTS Tokens response: {}", xstsTokenData);
                return false;
            }
            
            Result<String, ErrorWithCode> displayClaims = MicrosoftOAuthProcess.getUserHashFromDisplayClaims(xstsTokenData.get("DisplayClaims").getAsJsonObject());
            if (displayClaims.isErr()) {
                LOGGER.error("Failed to get user hash from display claims: {}", displayClaims.unwrapErr());
                return false;
            }
            
            LOGGER.info("Successfully refreshed XSTS token");
            this.xstsToken = xstsTokenData.get("Token").getAsString();
            this.xstsUserHash = displayClaims.unwrap();
            this.xstsNotAfter = Instant.parse(xstsTokenData.get("NotAfter").getAsString()).getEpochSecond();
            
            AccountManager.get().saveProfiles();
        }
        
        if (forceRefresh || now.getEpochSecond() >= this.minecraftAccessExpiresAt) {
            LOGGER.info("Minecraft token expired, refreshing");
            var minecraftResult = MicrosoftRequests.authenticateWithMinecraft(this.xstsToken, this.xstsUserHash);
            if (minecraftResult.isErr()) {
                LOGGER.error("Failed to authenticate with Minecraft: {}", minecraftResult.unwrapErr());
                return false;
            }

            JsonObject minecraftData = minecraftResult.unwrap();
            this.minecraftAccessToken = minecraftData.get("access_token").getAsString();
            this.minecraftAccessExpiresAt = now.plus(Duration.ofSeconds(minecraftData.get("expires_in").getAsLong())).getEpochSecond();
            
            // Now rerun the entitlements check and profile fetching
            var profile = MicrosoftOAuthProcess.checkEntitlementsAndGetProfile(this.minecraftAccessToken);
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
        AccountManager.get().saveProfiles();
        
        return true;
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

    public String getMinecraftAccessToken() {
        return minecraftAccessToken;
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
}
