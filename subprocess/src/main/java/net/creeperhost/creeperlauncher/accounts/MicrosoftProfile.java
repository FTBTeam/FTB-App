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
    private long xstsNotAfter;

    private String skinUrl;
    private boolean notLoggedIn = false;

    public MicrosoftProfile(UUID uuid, String minecraftName, String minecraftAccessToken, int minecraftAccessExpiresIn, String microsoftAccessToken, String microsoftRefreshToken, int microsoftAccessExpiresIn, String xstsUserHash, String xstsNotAfter, String skinUrl) {
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
        this.skinUrl = skinUrl;
    }
    
    /**
     * Checks to see if the account is still valid or if the tokens need to be refreshed
     */
    public boolean isValid() {
        if (this.notLoggedIn) {
            return false;
        }
        
        return Instant.now().getEpochSecond() < this.minecraftAccessExpiresAt;
    }
    
    /**
     * Refreshes the account if needed
     */
    public boolean refreshAccount(boolean forceRefresh) {
        try {
            boolean result = this.unsafeRefreshAccount(forceRefresh);
            if (!result) {
                this.notLoggedIn = true;
                AccountManager.get().saveProfiles();
            }
            
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
            var refreshResult = MicrosoftRequests.refreshWithXbox(this.microsoftRefreshToken);
            if (refreshResult.isErr()) {
                return false;
            }
            
            var jsonData = refreshResult.unwrap();
            this.microsoftAccessToken = jsonData.get("access_token").getAsString();
            this.microsoftRefreshToken = jsonData.get("refresh_token").getAsString();
            this.microsoftAccessExpiresAt = now.plus(Duration.ofSeconds(jsonData.get("expires_in").getAsLong())).getEpochSecond();
            AccountManager.get().saveProfiles();
        }
        
        if (forceRefresh || now.getEpochSecond() >= this.xstsNotAfter) {
            var xstsResult = MicrosoftRequests.authenticateWithXSTS(this.microsoftAccessToken);
            if (xstsResult.isErr()) {
                return false;
            }

            JsonObject xstsData = xstsResult.unwrap();
            if (!xstsData.has("DisplayClaims")) {
                return false;
            }
            
            Result<String, ErrorWithCode> displayClaims = MicrosoftOAuthProcess.getUserHashFromDisplayClaims(xstsData.get("DisplayClaims").getAsJsonObject());
            if (displayClaims.isErr()) {
                return false;
            }
            
            this.xstsUserHash = displayClaims.unwrap();
            this.xstsNotAfter = Instant.parse(xstsData.get("NotAfter").getAsString()).getEpochSecond();

            AccountManager.get().saveProfiles();
        }
        
        if (forceRefresh || now.getEpochSecond() >= this.minecraftAccessExpiresAt) {
            var minecraftResult = MicrosoftRequests.authenticateWithMinecraft(this.microsoftAccessToken, this.xstsUserHash);
            if (minecraftResult.isErr()) {
                return false;
            }

            JsonObject minecraftData = minecraftResult.unwrap();
            this.minecraftAccessToken = minecraftData.get("access_token").getAsString();
            this.minecraftAccessExpiresAt = now.plus(Duration.ofSeconds(minecraftData.get("expires_in").getAsLong())).getEpochSecond();
            
            // Now rerun the entitlements check and profile fetching
            var profile = MicrosoftOAuthProcess.checkEntitlementsAndGetProfile(this.minecraftAccessToken);
            if (profile.isErr()) {
                return false;
            }

            MinecraftProfileData profileData = profile.unwrap();
            
            this.minecraftName = profileData.name();
            this.skinUrl = profileData.getActiveSkinUrl();
            this.notLoggedIn = false;
        }
        
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
}
