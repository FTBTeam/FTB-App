package net.creeperhost.creeperlauncher.api.handlers.profiles;

import net.creeperhost.creeperlauncher.accounts.AccountManager;
import net.creeperhost.creeperlauncher.accounts.MicrosoftProfile;
import net.creeperhost.creeperlauncher.accounts.auth.MicrosoftOAuthProcess;
import net.creeperhost.creeperlauncher.accounts.data.OAuthTokenHolder;
import net.creeperhost.creeperlauncher.api.WebSocketHandler;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.data.PrivateBaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.util.MiscUtils;

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
