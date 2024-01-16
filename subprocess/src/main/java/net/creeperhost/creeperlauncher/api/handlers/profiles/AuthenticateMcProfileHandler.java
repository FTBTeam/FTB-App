package net.creeperhost.creeperlauncher.api.handlers.profiles;

import net.creeperhost.creeperlauncher.accounts.AccountManager;
import net.creeperhost.creeperlauncher.accounts.AccountProfile;
import net.creeperhost.creeperlauncher.accounts.authentication.ApiRecords;
import net.creeperhost.creeperlauncher.accounts.authentication.MicrosoftOAuth;
import net.creeperhost.creeperlauncher.accounts.authentication.MinecraftProfileData;
import net.creeperhost.creeperlauncher.accounts.authentication.MojangAuthenticator;
import net.creeperhost.creeperlauncher.accounts.data.ErrorWithCode;
import net.creeperhost.creeperlauncher.accounts.stores.AccountSkin;
import net.creeperhost.creeperlauncher.accounts.stores.YggdrasilAuthStore;
import net.creeperhost.creeperlauncher.api.WebSocketHandler;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.util.DataResult;
import net.creeperhost.creeperlauncher.util.MiscUtils;
import net.creeperhost.creeperlauncher.util.Result;

import java.time.Instant;

/**
 * @deprecated no longer needed 
 */
@Deprecated
public class AuthenticateMcProfileHandler implements IMessageHandler<AuthenticateMcProfileHandler.Data> {
    @Override
    public void handle(Data data) {
        MojangAuthenticator auth = new MojangAuthenticator();

        // Try and authenticate with the MC server
        // TODO: Use Result
        DataResult<YggdrasilAuthStore, ErrorWithCode> authenticate = auth.authenticate(new MojangAuthenticator.LoginData(data.username, data.password));

        authenticate.data().ifPresentOrElse(store -> {
            // Try and get the user profile with the access token from above
            Result<MinecraftProfileData, MicrosoftOAuth.RequestError> minecraftAccount = MicrosoftOAuth.fetchMcProfile(store.accessToken);

            // We've likely got a migration issue here
            if (minecraftAccount.isErr() && minecraftAccount.unwrapErr().status() == 403) {
                Result<ApiRecords.Responses.Migration, MicrosoftOAuth.RequestError> migrationStatus = MicrosoftOAuth.checkMigrationStatus(store.accessToken);
                // The user has to migrate, we should stop here.
                if (migrationStatus.isOk() && migrationStatus.unwrap().rollout) {
                    WebSocketHandler.sendMessage(new Reply(data, false, "You must migrate your account to a Microsoft account to continue playing Minecraft."));
                    return;
                }
            }

            if (minecraftAccount.isErr()) {
                WebSocketHandler.sendMessage(new Reply(data, false, "Failed to get Minecraft account info."));
                return;
            }
            
            var minecraftAccountData = minecraftAccount.unwrap();
            // No profile? Fail!
            // Pretty overkill here tbh 
            String userId = minecraftAccountData.id();
            if (userId == null || userId.isEmpty()) {
                WebSocketHandler.sendMessage(new Reply(data, false, "Failed to get Minecraft account info."));
                return;
            }

            // Parse the users skins (We don't care if it fails).
            AccountSkin[] skins = minecraftAccountData.skins().toArray(new AccountSkin[0]);
            
            // Create a generic profile
            AccountProfile profile = new AccountProfile(
                    MiscUtils.createUuidFromStringWithoutDashes(userId),
                    Instant.now().toEpochMilli(),
                    minecraftAccountData.name(),
                    skins,
                    new YggdrasilAuthStore(
                            store.accessToken, store.clientToken
                    )
            );

            // Try and add the profile
            AccountManager.get().addProfile(profile);
            WebSocketHandler.sendMessage(new Reply(data, true, "Success"));
        }, () -> {
            // Nope...
            WebSocketHandler.sendMessage(new Reply(data, false, authenticate.error().map(ErrorWithCode::error).orElse("Fatal error for Microsoft auth")));
        });
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
