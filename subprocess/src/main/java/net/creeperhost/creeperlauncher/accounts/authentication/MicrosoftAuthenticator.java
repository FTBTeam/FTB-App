package net.creeperhost.creeperlauncher.accounts.authentication;

import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.accounts.AccountProfile;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.util.Result;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
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

        boolean valid = Instant.now().getEpochSecond() < profile.msAuth.liveExpiresAt;
        LOGGER.info("Checking if Microsoft account is still valid... It's {}", valid ? "valid" : "invalid");
        return valid;
    }

    /**
     * Due to the way we handle authentication, this is basically ideal for a authentication, we only do the code twice to support different logger info.
     */
    @Nonnull
    @Override
    public Result<MicrosoftOAuth.DanceResult, MicrosoftOAuth.DanceCodedError> refresh(AccountProfile profile, AuthRequest refreshData) {
        return MicrosoftOAuth.
            create(new MicrosoftOAuth.DanceContext(refreshData.authToken, refreshData.liveRefreshToken, refreshData.liveExpiresAt), step -> {
                Settings.webSocketAPI.sendMessage(new StepProgressReply(step));
            })
            .runOAuthDance();
    }

    @Nonnull
    @Override
    public Result<MicrosoftOAuth.DanceResult, MicrosoftOAuth.DanceCodedError> authenticate(AuthRequest accessData) {
        return MicrosoftOAuth
            .create(new MicrosoftOAuth.DanceContext(accessData.authToken, accessData.liveRefreshToken, accessData.liveExpiresAt), step -> {
                Settings.webSocketAPI.sendMessage(new StepProgressReply(step));
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
