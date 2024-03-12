package net.creeperhost.creeperlauncher.api.handlers.profiles;

import net.creeperhost.creeperlauncher.accounts.AccountManager;
import net.creeperhost.creeperlauncher.accounts.AccountProfile;
import net.creeperhost.creeperlauncher.accounts.authentication.MicrosoftAuthenticator;
import net.creeperhost.creeperlauncher.accounts.authentication.MicrosoftOAuth;
import net.creeperhost.creeperlauncher.accounts.stores.AccountSkin;
import net.creeperhost.creeperlauncher.api.WebSocketHandler;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.data.PrivateBaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.util.Result;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

public class RefreshAuthenticationProfileHandler implements IMessageHandler<RefreshAuthenticationProfileHandler.Data> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(Data data) {
        if (data.profileUuid == null) {
            WebSocketHandler.sendMessage(new Reply(data, false, "Profile not found", false));
            return;
        }

        AccountProfile profile = AccountManager.get().getProfileFromUuid(data.profileUuid);
        if (profile == null) {
            WebSocketHandler.sendMessage(new Reply(data, false, "Profile not found", false));
            return;
        }

        MicrosoftAuthenticator authenticator = profile.getValidator();
        refreshMicrosoft(authenticator, data, profile);
    }

    /**
     * Refresh using Microsoft flow
     */
    private void refreshMicrosoft(MicrosoftAuthenticator validator, Data data, AccountProfile profile) {
        if (data.liveAccessToken == null) {
            WebSocketHandler.sendMessage(new Reply(data, false, "Missing essential information...", false));
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
            WebSocketHandler.sendMessage(new Reply(data, false, danceCodedError.code(), danceCodedError.networkError()));
            return;
        }

        // Update profile data as well as auth data
        profile.username = refresh.unwrap().profile().name();
        profile.skins = refresh.unwrap().profile().skins().toArray(new AccountSkin[0]);
        
        profile.msAuth = refresh.unwrap().store();
        AccountManager.get().saveProfiles();
        WebSocketHandler.sendMessage(new Reply(data, true, "", false));
    }

    private static class Reply extends PrivateBaseData {
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
