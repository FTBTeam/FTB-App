package net.creeperhost.creeperlauncher.api.handlers.profiles;

import com.google.gson.Gson;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.accounts.AccountManager;
import net.creeperhost.creeperlauncher.accounts.AccountProfile;
import net.creeperhost.creeperlauncher.accounts.authentication.AuthenticatorValidator;
import net.creeperhost.creeperlauncher.accounts.authentication.MicrosoftOAuth;
import net.creeperhost.creeperlauncher.accounts.authentication.MojangAuthenticator;
import net.creeperhost.creeperlauncher.accounts.stores.AccountSkin;
import net.creeperhost.creeperlauncher.accounts.stores.YggdrasilAuthStore;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.util.MiscUtils;

import java.time.Instant;
import java.util.UUID;

/**
 * The authentication server has CORS! Ughhh! Looks like we're doing this here now :P
 */
public class AuthenticateMcProfileHandler implements IMessageHandler<AuthenticateMcProfileHandler.Data> {
    @Override
    public void handle(Data data) {
        MojangAuthenticator auth = new MojangAuthenticator();

        // Try and authenticate with the MC server
        AuthenticatorValidator.Reply<YggdrasilAuthStore> authenticate =
                auth.authenticate(new MojangAuthenticator.LoginData(data.username, data.password));

        if (authenticate != null && authenticate.success()) {
            // Try and get the user profile with the access token from above
            MicrosoftOAuth.StepReply minecraftAccount = MicrosoftOAuth.getProfile(authenticate.data().accessToken);

            // We've likely got a migration issue here
            if (minecraftAccount.rawResponse() != null && minecraftAccount.rawResponse().code() == 403) {
                MicrosoftOAuth.StepReply migrationStatus = MicrosoftOAuth.checkMigrationStatus(authenticate.data().accessToken);
                // The user has to migrate, we should stop here.
                if (migrationStatus.success() && migrationStatus.data().getAsJsonObject().get("rollout").getAsBoolean()) {
                    Settings.webSocketAPI.sendMessage(new Reply(data, false, "You must migrate your account to a Microsoft account to continue playing Minecraft."));
                    return;
                }
            }

            // No profile? Fail!
            String userId = minecraftAccount.data().getAsJsonObject().get("id").getAsString();
            if (!minecraftAccount.success() || userId == null || userId.isEmpty()) {
                Settings.webSocketAPI.sendMessage(new Reply(data, false, "Failed to get Minecraft account info."));
                return;
            }

            // Parse the users skins (We don't care if it fails).
            AccountSkin[] skins = new AccountSkin[] {};
            try {
                skins = new Gson().fromJson(minecraftAccount.data().getAsJsonObject().get("skins").toString(), AccountSkin[].class);
            } catch (Exception ignore) {}


            // Create a generic profile
            AccountProfile profile = new AccountProfile(
                    MiscUtils.createUuidFromStringWithoutDashes(userId),
                    Instant.now().toEpochMilli(),
                    minecraftAccount.data().getAsJsonObject().get("name").getAsString(),
                    skins,
                    new YggdrasilAuthStore(
                            authenticate.data().accessToken, authenticate.data().clientToken
                    )
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
        public String username;
        public String password;
    }
}
