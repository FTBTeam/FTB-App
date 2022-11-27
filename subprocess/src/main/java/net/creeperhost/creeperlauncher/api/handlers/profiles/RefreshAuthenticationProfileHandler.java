package net.creeperhost.creeperlauncher.api.handlers.profiles;

import com.google.gson.JsonObject;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.accounts.AccountManager;
import net.creeperhost.creeperlauncher.accounts.AccountProfile;
import net.creeperhost.creeperlauncher.accounts.authentication.AuthenticatorValidator;
import net.creeperhost.creeperlauncher.accounts.authentication.MicrosoftAuthenticator;
import net.creeperhost.creeperlauncher.accounts.authentication.MojangAuthenticator;
import net.creeperhost.creeperlauncher.accounts.data.ErrorWithCode;
import net.creeperhost.creeperlauncher.accounts.stores.MSAuthStore;
import net.creeperhost.creeperlauncher.accounts.stores.YggdrasilAuthStore;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.util.DataResult;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;

/**
 * The authentication server has CORS! Ughhh! Looks like we're doing this here now :P
 */
public class RefreshAuthenticationProfileHandler implements IMessageHandler<RefreshAuthenticationProfileHandler.Data> {
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
            if (data.liveAccessToken == null) {
                Settings.webSocketAPI.sendMessage(new Reply(data, false, "Missing essential information...", false));
            }

            DataResult<Pair<JsonObject, MSAuthStore>, ErrorWithCode> refresh = validator.refresh(profile, new MicrosoftAuthenticator.AuthRequest(
                    data.liveAccessToken, data.liveRefreshToken, data.liveExpires
            ));

            refresh.data().ifPresentOrElse(d -> {
                profile.msAuth = d.getRight();
                AccountManager.get().saveProfiles();
                Settings.webSocketAPI.sendMessage(new Reply(data, true, "updated", false));
            }, () -> Settings.webSocketAPI.sendMessage(new Reply(data, true, "Unable to refresh: " + refresh.error().map(ErrorWithCode::error).orElse("Unknown error"), refresh.error().map(e -> e.rawReply() != null && e.rawReply().networkError()).orElse(false))));

            return;
        }

        MojangAuthenticator validator = (MojangAuthenticator) specificValidator;
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
        public String response;

        public Reply(Data data, boolean success, String rawResult, boolean networkError) {
            this.requestId = data.requestId;
            this.type = data.type + "Reply";

            this.success = success;
            this.response = rawResult;
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
