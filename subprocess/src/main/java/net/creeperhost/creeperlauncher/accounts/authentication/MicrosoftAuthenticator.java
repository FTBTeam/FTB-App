package net.creeperhost.creeperlauncher.accounts.authentication;

import net.creeperhost.creeperlauncher.accounts.AccountProfile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.time.Instant;

public class MicrosoftAuthenticator implements AuthenticatorValidator<AccountProfile.MSAuthStore, MicrosoftAuthenticator.RefreshData, MicrosoftAuthenticator.RefreshData> {
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

    @Nullable
    @Override
    public AuthenticatedWithData<AccountProfile.MSAuthStore> refresh(AccountProfile profile, RefreshData refreshData) {
        MicrosoftOAuth oauth = new MicrosoftOAuth();

        AccountProfile.MSAuthStore msAuthStore = oauth.runFlow(refreshData.authToken, refreshData.liveRefreshToken, refreshData.liveExpiresAt, LOGGER::info);
        return new AuthenticatedWithData<>(msAuthStore, msAuthStore != null, msAuthStore == null ? "Failed to refresh Microsoft authentication" : "Successfully refreshed Microsoft authentication");
    }

    @Nullable
    @Override
    public AuthenticatedWithData<AccountProfile.MSAuthStore> authenticate(RefreshData accessData) {
        MicrosoftOAuth oauth = new MicrosoftOAuth();

        AccountProfile.MSAuthStore msAuthStore = oauth.runFlow(accessData.authToken, accessData.liveRefreshToken, accessData.liveExpiresAt, LOGGER::info);
        return new AuthenticatedWithData<>(msAuthStore, msAuthStore != null, msAuthStore == null ? "Failed to authenticate Microsoft authentication" : "Successfully authenticated Microsoft authentication");
    }

    record RefreshData(
            String authToken, String liveRefreshToken, int liveExpiresAt
    ) {}
}
