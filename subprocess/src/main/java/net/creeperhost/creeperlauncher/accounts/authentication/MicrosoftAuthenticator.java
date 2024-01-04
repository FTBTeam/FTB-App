package net.creeperhost.creeperlauncher.accounts.authentication;

import net.covers1624.quack.gson.JsonUtils;
import net.creeperhost.creeperlauncher.accounts.AccountProfile;
import net.creeperhost.creeperlauncher.api.WebSocketHandler;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.util.Result;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;

public class MicrosoftAuthenticator implements AuthenticatorValidator<
    Result<MicrosoftOAuth.DanceResult, MicrosoftOAuth.DanceCodedError>, 
    MicrosoftAuthenticator.AuthRequest, 
    MicrosoftAuthenticator.AuthRequest
> {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public boolean isValid(AccountProfile profile) {
        if (profile.msAuth == null) {
            LOGGER.warn("Attempted to check Mojang account for valid Microsoft authentication... uuid: {}", profile.uuid);
            return false;
        }

        // Is valid if the expire token is in the future minus 30 minutes
        boolean valid = Instant.now().getEpochSecond() < profile.msAuth.liveExpiresAt - (60 * 30);
        int mcTokenExpires = extractExpiresFromToken(profile.msAuth.minecraftToken);
        if (mcTokenExpires != -1) {
            boolean mcValid = Instant.now().getEpochSecond() < mcTokenExpires - (60 * 30);
            LOGGER.info("Found exp for Minecraft token: {}, has expired {}", mcTokenExpires, !mcValid ? "YES" : "NO");
            valid = valid && mcValid;
        }
        
        LOGGER.info("Checking if Microsoft account is expired... It's {}", valid ? "OK" : "Expired");
        if (!valid) return false;

        LOGGER.info("Checking if Minecraft token is usable....");
        var result = MicrosoftOAuth.fetchMcProfile(profile.msAuth.minecraftToken);
        valid = result.isOk();
        LOGGER.info("It's {}", valid ? "OK" : "Invalid: " + result.unwrapErr().status() + " : "+ result.unwrapErr().body());
        return valid;
    }
    
    private static int extractExpiresFromToken(String jwt) {
        var parts = jwt.split("\\.");
        if (parts.length != 3) {
            LOGGER.warn("Invalid JWT, expected 3 parts, got {}", parts.length);
            return -1;
        }
        
        // Payload is the second part
        var payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
        
        // Parse the json
        try {
            var jsonData = JsonUtils.parseRaw(payload);
            if (!jsonData.getAsJsonObject().has("exp")) {
                LOGGER.warn("JWT payload does not contain 'exp' field");
                return -1;
            }
            
            return jsonData.getAsJsonObject().get("exp").getAsInt();
        } catch (Exception e) {
            LOGGER.warn("Failed to parse JWT payload", e);
            return -1;
        }
    }
    
    /**
     * Due to the way we handle authentication, this is basically ideal for a authentication, we only do the code twice to support different logger info.
     */
    @Nonnull
    @Override
    public Result<MicrosoftOAuth.DanceResult, MicrosoftOAuth.DanceCodedError> refresh(AccountProfile profile, AuthRequest refreshData) {
        return MicrosoftOAuth.
            create(new MicrosoftOAuth.DanceContext(refreshData.authToken, refreshData.liveRefreshToken, refreshData.liveExpiresAt), step -> {
                WebSocketHandler.sendMessage(new StepProgressReply(step));
            })
            .runOAuthDance();
    }

    @Nonnull
    @Override
    public Result<MicrosoftOAuth.DanceResult, MicrosoftOAuth.DanceCodedError> authenticate(AuthRequest accessData) {
        return MicrosoftOAuth
            .create(new MicrosoftOAuth.DanceContext(accessData.authToken, accessData.liveRefreshToken, accessData.liveExpiresAt), step -> {
                WebSocketHandler.sendMessage(new StepProgressReply(step));
            })
            .runOAuthDance();
    }
    
    private static class StepProgressReply extends BaseData {
        public String id;
        public boolean working;
        public boolean successful;
        public boolean error;

        public StepProgressReply(MicrosoftOAuth.DanceStep step) {
            this.type = "authenticationStepUpdate";
            this.id = step.step().name();
            this.working = step.working();
            this.successful = step.successful();
            this.error = step.error();
        }
    }

    public record AuthRequest(
            String authToken, 
            String liveRefreshToken, 
            int liveExpiresAt
    ) {}
}
