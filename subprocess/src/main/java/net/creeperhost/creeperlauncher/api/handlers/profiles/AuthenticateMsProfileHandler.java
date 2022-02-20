package net.creeperhost.creeperlauncher.api.handlers.profiles;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.accounts.AccountManager;
import net.creeperhost.creeperlauncher.accounts.AccountProfile;
import net.creeperhost.creeperlauncher.accounts.authentication.AuthenticatorValidator;
import net.creeperhost.creeperlauncher.accounts.authentication.MicrosoftAuthenticator;
import net.creeperhost.creeperlauncher.accounts.stores.AccountSkin;
import net.creeperhost.creeperlauncher.accounts.stores.MSAuthStore;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import org.apache.commons.lang3.tuple.Pair;

import java.time.Instant;

/**
 * The authentication server has CORS! Ughhh! Looks like we're doing this here now :P
 */
public class AuthenticateMsProfileHandler implements IMessageHandler<AuthenticateMsProfileHandler.Data> {
    @Override
    public void handle(Data data) {
        MicrosoftAuthenticator auth = new MicrosoftAuthenticator();

        // Try and authenticate with the MC server
        AuthenticatorValidator.Reply<Pair<JsonObject, MSAuthStore>> authenticate =
                auth.authenticate(new MicrosoftAuthenticator.AuthRequest(data.liveAccessToken, data.liveRefreshToken, data.liveExpires));

        if (authenticate != null && authenticate.success()) {
            // Parse the users skins (We don't care if it fails).
            AccountSkin[] skins = new AccountSkin[] {};
            try {
                skins = new Gson().fromJson(authenticate.data().getKey().getAsJsonObject().get("skins").toString(), AccountSkin[].class);
            } catch (Exception ignore) {}

            AccountProfile profile = new AccountProfile(
                    authenticate.data().getValue().minecraftUuid,
                    Instant.now().getEpochSecond(),
                    authenticate.data().getKey().getAsJsonObject().get("name").getAsString(),
                    skins,
                    authenticate.data().getValue()
            );

            // Try and add the profile
            AccountManager.get().addProfile(profile);

            Settings.webSocketAPI.sendMessage(new Reply(data, true, "Success"));
            return;
        }

        // Nope...
        Settings.webSocketAPI.sendMessage(new Reply(data, false, authenticate == null ? "Failed to authenticate with Minecraft server." : authenticate.message()));
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
        public String liveAccessToken;
        public String liveRefreshToken;
        public int liveExpires;
    }
}
