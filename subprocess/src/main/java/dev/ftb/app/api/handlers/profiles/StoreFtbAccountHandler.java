package dev.ftb.app.api.handlers.profiles;

import com.google.gson.Gson;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.data.PrivateBaseData;
import dev.ftb.app.api.data.other.RequestInstanceRefresh;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.storage.CredentialStorage;
import dev.ftb.app.util.ModpackApiUtils;

import java.time.Instant;

public class StoreFtbAccountHandler implements IMessageHandler<StoreFtbAccountHandler.Data> {
    @Override
    public void handle(Data data) {
        CredentialStorage instance = CredentialStorage.getInstance();
        
        CompleteTokenData completeToken = new CompleteTokenData();
        
        completeToken.token = data.token;
        completeToken.idToken = data.idToken;
        // Update the API token
        ModpackApiUtils.API_TOKEN = data.idToken;
        completeToken.refreshToken = data.refreshToken;
        completeToken.expiresIn = data.expiresIn;
        completeToken.refreshExpiresIn = data.refreshExpiresIn;
        
        // Computed at login time
        completeToken.loggedInAt = Instant.now().getEpochSecond();
        completeToken.expiresAt = completeToken.loggedInAt + data.expiresIn;
        completeToken.refreshExpiresAt = data.refreshExpiresIn == 0 ? 0 : completeToken.loggedInAt + data.refreshExpiresIn;
        
        // Encode the data given to json
        var json = new Gson().toJson(completeToken);
        
        // Meh, just store it as json.
        instance.set("ftbAccount", json);
        
        // Return the token back for convenience
        WebSocketHandler.sendMessage(new Reply(data, completeToken));
        WebSocketHandler.sendMessage(new RequestInstanceRefresh());
    }

    // This data is used, we just json it so we don't need to worry about it
    @SuppressWarnings("unused")
    public static class Data extends PrivateBaseData {
        public String token;
        public String idToken;
        public String refreshToken;
        
        public int expiresIn;
        public int refreshExpiresIn;
    }
    
    public static class CompleteTokenData {
        public String token;
        public String idToken;
        public String refreshToken;

        public int expiresIn;
        public int refreshExpiresIn;
        
        public long loggedInAt;
        public long expiresAt;
        public long refreshExpiresAt;
    }
    
    public static class Reply extends Data {
        public boolean success;
        public CompleteTokenData completeToken;
        
        public Reply(Data data, CompleteTokenData completeToken) {
            this.type = data.type + "Reply";
            this.requestId = data.requestId;
            this.success = true;
            this.completeToken = completeToken;
        }
    }
}
