package net.creeperhost.creeperlauncher.api.handlers.profiles;

import com.google.gson.JsonObject;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.accounts.AccountManager;
import net.creeperhost.creeperlauncher.accounts.AccountProfile;
import net.creeperhost.creeperlauncher.accounts.authentication.AuthenticatorValidator;
import net.creeperhost.creeperlauncher.accounts.authentication.MicrosoftAuthenticator;
import net.creeperhost.creeperlauncher.accounts.authentication.MojangAuthenticator;
import net.creeperhost.creeperlauncher.accounts.stores.MSAuthStore;
import net.creeperhost.creeperlauncher.accounts.stores.YggdrasilAuthStore;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;

/**
 * The authentication server has CORS! Ughhh! Looks like we're doing this here now :P
 */
public class RefreshAuthenticationProfileHandler implements IMessageHandler<RefreshAuthenticationProfileHandler.Data> {
    @Override
    public void handle(Data data) {
        if (data.profileUuid == null) {
            Settings.webSocketAPI.sendMessage(new Reply(data, false, "Profile not found"));
            return;
        }

        AccountProfile profile = AccountManager.get().getProfileFromUuid(data.profileUuid);
        if (profile == null) {
            Settings.webSocketAPI.sendMessage(new Reply(data, false, "Profile not found"));
            return;
        }

        AuthenticatorValidator<?, ?, ?> specificValidator = profile.getValidator();
        if (specificValidator instanceof MicrosoftAuthenticator validator) {
            if (data.liveAccessToken == null) {
                Settings.webSocketAPI.sendMessage(new Reply(data, false, "Missing essential information..."));
            }

            AuthenticatorValidator.Reply<Pair<JsonObject, MSAuthStore>> refresh = validator.refresh(profile, new MicrosoftAuthenticator.AuthRequest(
                    data.liveAccessToken, data.liveRefreshToken, data.liveExpires
            ));

            if (refresh != null && refresh.success()) {
                // TODO: Check me
                profile.msAuth = refresh.data().getRight();
                AccountManager.get().saveProfiles();
                Settings.webSocketAPI.sendMessage(new Reply(data, true, "updated"));
                return;
            }

            Settings.webSocketAPI.sendMessage(new Reply(data, true, "Unable to refresh: " + (refresh != null ? refresh.message() : "Unknown error")));
            return;
        }

        MojangAuthenticator validator = (MojangAuthenticator) specificValidator;
        AuthenticatorValidator.Reply<YggdrasilAuthStore> refresh = validator.refresh(profile, "ignore me");

        if (refresh != null && refresh.success()) {
            profile.mcAuth = refresh.data();
            AccountManager.get().saveProfiles();
            Settings.webSocketAPI.sendMessage(new Reply(data, true, "updated"));
            return;
        }

        Settings.webSocketAPI.sendMessage(new Reply(data, true, "Unable to refresh: " + (refresh != null ? refresh.message() : "Unknown error")));
    }

    private static class Reply extends Data {
        public boolean success;
        public String response;

        public Reply(Data data, boolean success, String rawResult) {
            this.requestId = data.requestId;
            this.type = data.type + "Reply";

            this.success = success;
            this.response = rawResult;
        }
    }

    public static class Data extends BaseData {
        public String profileUuid;

        @Nullable public String liveAccessToken;
        @Nullable public String liveRefreshToken;
        public int liveExpires = -1;
    }
}
