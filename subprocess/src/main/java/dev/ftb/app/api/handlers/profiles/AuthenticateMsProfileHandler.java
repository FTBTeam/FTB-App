package dev.ftb.app.api.handlers.profiles;

import dev.ftb.app.accounts.AccountManager;
import dev.ftb.app.accounts.MicrosoftProfile;
import dev.ftb.app.accounts.auth.MicrosoftOAuthProcess;
import dev.ftb.app.accounts.data.OAuthTokenHolder;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.data.PrivateBaseData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.util.MiscUtils;

import javax.annotation.Nullable;

public class AuthenticateMsProfileHandler implements IMessageHandler<AuthenticateMsProfileHandler.Data> {
    @Override
    public void handle(Data data) {
        var result = MicrosoftOAuthProcess.authWithMinecraft(new OAuthTokenHolder(
                data.liveAccessToken,
                data.liveRefreshToken,
                data.liveExpires
        ));
        
        if (result.isErr()) {
            var error = result.unwrapErr();
            WebSocketHandler.sendMessage(new Reply(data, false, error.code(), error.message()));
            return;
        }
        
        var accountData = result.unwrap();
        
        var newAccount = new MicrosoftProfile(
            MiscUtils.createUuidFromStringWithoutDashes(accountData.profileData().id()),
            accountData.profileData().name(),
            accountData.minecraftAccessToken(),
            accountData.minecraftExpiresIn(),
            data.liveAccessToken,
            data.liveRefreshToken,
            data.liveExpires,
            accountData.userHash(),
            accountData.notAfter(),
            accountData.xstsToken(),
            accountData.profileData().getActiveSkinUrl()
        );
        
        AccountManager.get().addProfile(newAccount);

        WebSocketHandler.sendMessage(new Reply(data));
    }

    private static class Reply extends PrivateBaseData {
        public boolean success;
        @Nullable public String code;
        @Nullable public String message;

        public Reply(Data data, boolean success, @Nullable String code, @Nullable String message) {
            this.requestId = data.requestId;
            this.type = data.type + "Reply";

            this.success = success;
            this.code = code;
            this.message = message;
        }

        public Reply(Data data) {
            this(data, true, null, null);
        }
    }

    public static class Data extends BaseData {
        public String liveAccessToken;
        public String liveRefreshToken;
        public int liveExpires;
    }
}
