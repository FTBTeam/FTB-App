package net.creeperhost.creeperlauncher.api.handlers.other;

import com.google.gson.Gson;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.api.WebSocketHandler;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.api.handlers.other.minetogether.MineTogetherApi;
import net.creeperhost.creeperlauncher.api.handlers.other.minetogether.MineTogetherAuthenticationHandler;
import net.creeperhost.creeperlauncher.api.handlers.other.minetogether.MineTogetherProfile;
import net.creeperhost.creeperlauncher.storage.CredentialStorage;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;

public class AppInitHandler implements IMessageHandler<AppInitHandler.Data> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppInitHandler.class);
    
    @Override
    public void handle(Data data) {
        // Attempt login to MT if we have a token saved. If it fails, clear it
        String mtToken = CredentialStorage.getInstance().get("minetogether");
 
        boolean success = true;
        Reply.Builder reply = Reply.builder(data);
        if (mtToken != null && !mtToken.isEmpty()) {
            var loginResult = MineTogetherAuthenticationHandler.login(CredentialStorage.getInstance().get("minetogether"), true);
            if (!loginResult.isErr()) {
                var loginData = loginResult.unwrap();
                if (loginData.getKey() != null) {
                    reply.setBasicData(loginData.getKey());
                }
                
                if (loginData.getValue() != null) {
                    reply.setProfile(loginData.getValue());
                }
            }
        }
        
        UserApiCredentials userApiCredentials = this.loadUserApiCredentials();
        if (userApiCredentials != null) {
            Constants.KEY = userApiCredentials.apiSecret;
            reply.setUserApiCredentials(userApiCredentials);
        }
        
        reply.setSuccess(success);
        WebSocketHandler.sendMessage(reply.build());
    }

    private UserApiCredentials loadUserApiCredentials() {
        if (Files.notExists(Constants.USER_PROVIDED_API_CREDENTIALS_FILE)) {
            return null;
        }
        
        try {
            return new Gson().fromJson(Files.readString(Constants.USER_PROVIDED_API_CREDENTIALS_FILE), UserApiCredentials.class);
        } catch (Exception e) {
            LOGGER.error("Failed to load user provided API credentials", e);
            return null;
        }
    }
    
    public static class Data extends BaseData {}
    
    public static class Reply extends Data {
        boolean success = false;
        String errorMessage = "";
        @Nullable MineTogetherApi.BasicDataAndAccount basicData;
        @Nullable MineTogetherProfile profile;
        @Nullable UserApiCredentials apiCredentials;
        
        private Reply(Data data) {
            super();
            this.requestId = data.requestId;
            this.type = "appInitReply";
        }
        
        public static Builder builder(Data data) {
            return new Builder(data);
        }
        
        public static class Builder {
            private final Reply reply;
            
            private Builder(Data data) {
                reply = new Reply(data);
            }
            
            public Builder setBasicData(MineTogetherApi.BasicDataAndAccount basicData) {
                reply.basicData = basicData;
                return this;
            }
            
            public Builder setProfile(MineTogetherProfile profile) {
                reply.profile = profile;
                return this;
            }
            
            public Builder setSuccess(boolean success) {
                reply.success = success;
                return this;
            }
            
            public Builder setErrorMessage(String errorMessage) {
                reply.errorMessage = errorMessage;
                return this;
            }
            
            public Builder setUserApiCredentials(UserApiCredentials userApiCredentials) {
                reply.apiCredentials = userApiCredentials;
                return this;
            }
            
            public Reply build() {
                return reply;
            }
        }
    }
    
    record UserApiCredentials(
        String apiUrl,
        String apiSecret,
        ApiSettings settings
    ) {
        
        static class ApiSettings {
            boolean useAuthorizationHeader = false;
            boolean useAuthorizationAsBearer = false;
            boolean usePublicUrl = true;
        } 
    }
}
