package dev.ftb.app.api.handlers.profiles;

import com.google.gson.Gson;
import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.storage.CredentialStorage;

public class StoreFtbAccountHandler implements IMessageHandler<StoreFtbAccountHandler.Data> {
    @Override
    public void handle(Data data) {
        CredentialStorage instance = CredentialStorage.getInstance();
        
        // Encode the data given to json
        var json = new Gson().toJson(data);
        
        // Meh, just store it as json.
        instance.set("ftbAccount", json);
    }

    public static class Data extends BaseData {
        public String token;
        public String idToken;
        public String refreshToken;
        
        public int expiresIn;
        public int refreshExpiresIn;
    }
    
    public static class Reply extends Data {
        public boolean success;
        
        public Reply(Data data) {
            this.type = data.type + "Reply";
            this.requestId = data.requestId;
            this.success = true;
        }
    }
}
