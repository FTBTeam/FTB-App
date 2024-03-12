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

import javax.annotation.Nullable;
import java.time.Instant;

/**
 * The authentication server has CORS! Ughhh! Looks like we're doing this here now :P
 */
public class AuthenticateMsProfileHandler implements IMessageHandler<AuthenticateMsProfileHandler.Data> {
    @Override
    public void handle(Data data) {
        MicrosoftAuthenticator auth = new MicrosoftAuthenticator();

        // Try and authenticate with the MC server
        Result<MicrosoftOAuth.DanceResult, MicrosoftOAuth.DanceCodedError> authenticate = auth.authenticate(new MicrosoftAuthenticator.AuthRequest(data.liveAccessToken, data.liveRefreshToken, data.liveExpires));

        if (authenticate.isErr()) {
            MicrosoftOAuth.DanceCodedError danceCodedError = authenticate.unwrapErr();
            WebSocketHandler.sendMessage(new Reply(data, false, danceCodedError.code(), danceCodedError.networkError()));
            return;
        }

        MicrosoftOAuth.DanceResult result = authenticate.unwrap();
        // Parse the users skins (We don't care if it fails).
        AccountSkin[] skins = result.profile().skins().toArray(new AccountSkin[0]);
        AccountProfile profile = new AccountProfile(
                result.store().minecraftUuid,
                Instant.now().getEpochSecond(),
                result.profile().name(),
                skins,
                result.store()
        );

        // Try and add the profile
        AccountManager.get().addProfile(profile);

        WebSocketHandler.sendMessage(new Reply(data));
    }

    private static class Reply extends PrivateBaseData {
        public boolean success;
        public boolean networkError;
        @Nullable public String code;

        public Reply(Data data, boolean success, @Nullable String code, boolean networkError) {
            this.requestId = data.requestId;
            this.type = data.type + "Reply";

            this.success = success;
            this.code = code;
            this.networkError = networkError;
        }

        public Reply(Data data) {
            this(data, true, null, false);
        }
    }

    public static class Data extends BaseData {
        public String liveAccessToken;
        public String liveRefreshToken;
        public int liveExpires;
    }
}
