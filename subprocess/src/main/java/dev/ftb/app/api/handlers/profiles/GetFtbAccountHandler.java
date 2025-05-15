package dev.ftb.app.api.handlers.profiles;

import com.google.gson.Gson;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.data.PrivateBaseData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.storage.CredentialStorage;
import org.jetbrains.annotations.Nullable;

public class GetFtbAccountHandler implements IMessageHandler<BaseData> {
    @Override
    public void handle(BaseData data) {
        CredentialStorage instance = CredentialStorage.getInstance();
        var accountData = instance.get("ftbAccount");
        if (accountData == null) {
            WebSocketHandler.sendMessage(new Reply(data, null));
            return;
        }
        
        // Deserialize the account data
        StoreFtbAccountHandler.CompleteTokenData authData = new Gson().fromJson(accountData, StoreFtbAccountHandler.CompleteTokenData.class);
        if (authData == null) {
            WebSocketHandler.sendMessage(new Reply(data, null));
            return;
        }
        
        WebSocketHandler.sendMessage(new Reply(data, authData));
    }
    
    public static class Reply extends PrivateBaseData {
        public boolean success;
        @Nullable
        public StoreFtbAccountHandler.CompleteTokenData authData;
        
        public Reply(BaseData data, @Nullable  StoreFtbAccountHandler.CompleteTokenData authData) {
            this.type = data.type + "Reply";
            this.requestId = data.requestId;
            this.success = authData != null;
            this.authData = authData;
        }
    }
}
