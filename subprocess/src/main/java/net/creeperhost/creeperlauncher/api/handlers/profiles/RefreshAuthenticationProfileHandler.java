package net.creeperhost.creeperlauncher.api.handlers.profiles;

import com.google.gson.JsonObject;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.accounts.AccountManager;
import net.creeperhost.creeperlauncher.accounts.AccountProfile;
import net.creeperhost.creeperlauncher.accounts.authentication.AuthenticatorValidator;
import net.creeperhost.creeperlauncher.accounts.authentication.MicrosoftAuthenticator;
import net.creeperhost.creeperlauncher.accounts.authentication.MicrosoftOAuth;
import net.creeperhost.creeperlauncher.accounts.authentication.MojangAuthenticator;
import net.creeperhost.creeperlauncher.accounts.data.ErrorWithCode;
import net.creeperhost.creeperlauncher.accounts.stores.MSAuthStore;
import net.creeperhost.creeperlauncher.accounts.stores.YggdrasilAuthStore;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.util.DataResult;
import net.creeperhost.creeperlauncher.util.Result;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

/**
 * The authentication server has CORS! Ughhh! Looks like we're doing this here now :P
 */
public class RefreshAuthenticationProfileHandler implements IMessageHandler<RefreshAuthenticationProfileHandler.Data> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(Data data) {
        if (data.profileUuid == null) {
            Settings.webSocketAPI.sendMessage(new Reply(data, false, "Profile not found", false));
            return;
        }

        AccountProfile profile = AccountManager.get().getProfileFromUuid(data.profileUuid);
        if (profile == null) {
            Settings.webSocketAPI.sendMessage(new Reply(data, false, "Profile not found", false));
            return;
        }

        AuthenticatorValidator<?, ?, ?> specificValidator = profile.getValidator();
        if (specificValidator instanceof MicrosoftAuthenticator validator) {
            refreshMicrosoft(validator, data, profile);
            return;
        }

        refreshMinecraft((MojangAuthenticator) specificValidator, data, profile);
    }

    /**
     * Refresh using Microsoft flow
     */
    private void refreshMicrosoft(MicrosoftAuthenticator validator, Data data, AccountProfile profile) {
        if (data.liveAccessToken == null) {
            Settings.webSocketAPI.sendMessage(new Reply(data, false, "Missing essential information...", false));
        }

        Result<MicrosoftOAuth.DanceResult, MicrosoftOAuth.DanceCodedError> refresh = null;
        int tries = 0;
        while (refresh == null || (refresh.isErr() && tries++ < 5)) {
            LOGGER.info("Trying to refresh {}/5", tries);
            if (tries > 0) {
                try {
                    Thread.sleep(tries * 1000L);
                } catch (InterruptedException ignored) { }
            }
            refresh = validator.refresh(profile, new MicrosoftAuthenticator.AuthRequest(
                    data.liveAccessToken, data.liveRefreshToken, data.liveExpires
            ));
            if (refresh.isErr()) {
                MicrosoftOAuth.DanceCodedError danceCodedError = refresh.unwrapErr();
                LOGGER.warn("Refresh error: {} {}", danceCodedError.code(), danceCodedError.networkError());
            }
        }

        if (refresh.isErr()) {
            MicrosoftOAuth.DanceCodedError danceCodedError = refresh.unwrapErr();
            LOGGER.warn("Did not get valid token. :( {} {}", danceCodedError.code(), danceCodedError.networkError());
            Settings.webSocketAPI.sendMessage(new Reply(data, false, danceCodedError.code(), danceCodedError.networkError()));
            return;
        }

        profile.msAuth = refresh.unwrap().store();
        AccountManager.get().saveProfiles();
        Settings.webSocketAPI.sendMessage(new Reply(data, true, "", false));
    }

    /**
     * Refresh using the Minecraft flow
     * 
     * @implNote Minecraft auth is being yeeted
     */
    private void refreshMinecraft(MojangAuthenticator validator, Data data, AccountProfile profile) {
        DataResult<YggdrasilAuthStore, ErrorWithCode> refresh = validator.refresh(profile, "ignore me");

        refresh.data().ifPresentOrElse(d -> {
            profile.mcAuth = d;
            AccountManager.get().saveProfiles();
            Settings.webSocketAPI.sendMessage(new Reply(data, true, "updated", false));
        }, () -> Settings.webSocketAPI.sendMessage(new Reply(data, false, "Unable to refresh: " + refresh.error().map(ErrorWithCode::error).orElse("Unknown error"), false)));
    }

    private static class Reply extends Data {
        public boolean networkError;
        public boolean success;
        public String code;

        public Reply(Data data, boolean success, String code, boolean networkError) {
            this.requestId = data.requestId;
            this.type = data.type + "Reply";

            this.success = success;
            this.code = code;
            this.networkError = networkError;
        }
    }

    public static class Data extends BaseData {
        public String profileUuid;

        @Nullable public String liveAccessToken;
        @Nullable public String liveRefreshToken;
        public int liveExpires = -1;
    }
}
