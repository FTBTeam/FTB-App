package net.creeperhost.creeperlauncher.accounts.authentication;

import com.google.gson.JsonObject;
import net.creeperhost.creeperlauncher.accounts.AccountProfile;
import net.creeperhost.creeperlauncher.accounts.data.ErrorWithCode;
import net.creeperhost.creeperlauncher.accounts.stores.MSAuthStore;
import net.creeperhost.creeperlauncher.util.DataResult;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;

public class MicrosoftAuthenticator implements AuthenticatorValidator<DataResult<Pair<JsonObject, MSAuthStore>, ErrorWithCode>, MicrosoftAuthenticator.AuthRequest, MicrosoftAuthenticator.AuthRequest> {
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
     * @param profile
     * @param refreshData
     * @return
     */
    @Nonnull
    @Override
    public DataResult<Pair<JsonObject, MSAuthStore>, ErrorWithCode> refresh(AccountProfile profile, AuthRequest refreshData) {
        return MicrosoftOAuth.runFlow(refreshData.authToken, refreshData.liveRefreshToken, refreshData.liveExpiresAt);
    }

    @Nonnull
    @Override
    public DataResult<Pair<JsonObject, MSAuthStore>, ErrorWithCode> authenticate(AuthRequest accessData) {
        return MicrosoftOAuth.runFlow(accessData.authToken, accessData.liveRefreshToken, accessData.liveExpiresAt);
    }

    public record AuthRequest(
            String authToken, String liveRefreshToken, int liveExpiresAt
    ) {}
}
