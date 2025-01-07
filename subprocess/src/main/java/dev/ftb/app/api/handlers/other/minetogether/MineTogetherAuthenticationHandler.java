package dev.ftb.app.api.handlers.other.minetogether;

import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.data.PrivateBaseData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.storage.CredentialStorage;
import dev.ftb.app.util.ModpacksChUtils;
import dev.ftb.app.util.Result;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.StringJoiner;

public class MineTogetherAuthenticationHandler implements IMessageHandler<MineTogetherAuthenticationHandler.Data> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MineTogetherAuthenticationHandler.class);
    
    @Override
    public void handle(Data data) {
        var loginResult = login(data.appToken, true);
        if (data.appToken == null) {
            WebSocketHandler.sendMessage(new Reply(data, false,null, null, "No app token provided"));
            return;
        }
  
        if (loginResult.isErr()) {
            WebSocketHandler.sendMessage(new Reply(data, false, null, null, loginResult.unwrapErr()));
            return;
        }
        
        var login = loginResult.unwrap();
        var user = login.getLeft();
        var profile = login.getRight();
        
        WebSocketHandler.sendMessage(new Reply(data, true, user, profile, null));
    }
    
    public static Result<Pair<MineTogetherApi.BasicDataAndAccount, MineTogetherProfile>, String> login(String token, boolean isFirst) {
        var currentUser = MineTogetherApi.getCurrentUser(token, !isFirst);

        if (currentUser.isErr()) {
            String error = currentUser.unwrapErr();
            LOGGER.error("Failed to get current user: {}", error);
            return Result.err(error);
        }

        var user = currentUser.unwrap();
        var profile = MineTogetherApi.profile(user.data().uuid());
        var profileOrNull = profile.isErr() ? null : profile.unwrap();
        if (profile.isErr()) {
            LOGGER.error("Failed to get profile: {}", profile.unwrapErr());
        }

        ModpacksChUtils.API_TOKEN = user.data().modpacksToken();

        CredentialStorage.getInstance().set("minetogether", token);
        CredentialStorage.getInstance().set("modpacksChApiKey", user.data().modpacksToken());

        return Result.ok(Pair.of(user, profileOrNull));
    }

    public static class Data extends BaseData {
        String authType;
        String apiKey;
        String appToken;

        @Override
        public String toString() {
            return new StringJoiner(", ", Data.class.getSimpleName() + "[", "]")
                .add("authType='" + authType + "'")
                .add("apiKey='" + apiKey + "'") // Don't use, no clue what this is used for
                .add("appToken='" + appToken + "'") // Use. Pretty sure this in the PHPSID from the headers
                .toString();
        }
    }

    public static class Reply extends PrivateBaseData {
        boolean success;
        @Nullable String message;
        @Nullable MineTogetherApi.BasicDataAndAccount basicData;
        @Nullable MineTogetherProfile profile;
        
        public Reply(Data data, boolean success, @Nullable MineTogetherApi.BasicDataAndAccount profileData, @Nullable MineTogetherProfile profile, @Nullable String message) {
            this.requestId = data.requestId;
            this.type = "mineTogetherAuthenticationReply";
            
            this.success = success;
            this.basicData = profileData;
            this.profile = profile;
            this.message = message;
        }
    }
}
