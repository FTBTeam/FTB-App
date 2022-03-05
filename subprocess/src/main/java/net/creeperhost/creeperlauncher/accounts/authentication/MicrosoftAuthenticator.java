package net.creeperhost.creeperlauncher.accounts.authentication;

import com.google.gson.JsonObject;
import net.creeperhost.creeperlauncher.accounts.AccountProfile;
import net.creeperhost.creeperlauncher.accounts.stores.MSAuthStore;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.time.Instant;

public class MicrosoftAuthenticator implements AuthenticatorValidator<Pair<JsonObject, MSAuthStore>, MicrosoftAuthenticator.AuthRequest, MicrosoftAuthenticator.AuthRequest> {
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
    @Nullable
    @Override
    public Reply<Pair<JsonObject, MSAuthStore>> refresh(AccountProfile profile, AuthRequest refreshData) {
        Pair<JsonObject, MSAuthStore> msAuthStore = MicrosoftOAuth.runFlow(refreshData.authToken, refreshData.liveRefreshToken, refreshData.liveExpiresAt, this::log);
        return new Reply<>(msAuthStore, msAuthStore != null, msAuthStore == null ? "Failed to refresh Microsoft authentication" : "Successfully refreshed Microsoft authentication");
    }

    @Nullable
    @Override
    public Reply<Pair<JsonObject, MSAuthStore>> authenticate(AuthRequest accessData) {
        Pair<JsonObject, MSAuthStore> msAuthStore = MicrosoftOAuth.runFlow(accessData.authToken, accessData.liveRefreshToken, accessData.liveExpiresAt, this::log);
        return new Reply<>(msAuthStore, msAuthStore != null, msAuthStore == null ? "Failed to authenticate Microsoft authentication" : "Successfully authenticated Microsoft authentication");
    }

    /**
     * Not super graceful.
     */
    private void log(Pair<Boolean, String> message) {
        if (message.getLeft()) {
            LOGGER.info(message.getRight());
        } else {
            LOGGER.error(message.getRight());
        }
    }

    public record AuthRequest(
            String authToken, String liveRefreshToken, int liveExpiresAt
    ) {}
}
