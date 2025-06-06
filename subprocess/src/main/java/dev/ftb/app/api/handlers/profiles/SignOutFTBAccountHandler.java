package dev.ftb.app.api.handlers.profiles;

import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.data.other.RequestInstanceRefresh;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.storage.CredentialStorage;
import dev.ftb.app.util.ModpackApiUtils;

public class SignOutFTBAccountHandler implements IMessageHandler<BaseData> {
    @Override
    public void handle(BaseData data) {
        CredentialStorage.getInstance().remove("ftbAccount");
        ModpackApiUtils.API_TOKEN = null;
        WebSocketHandler.sendMessage(new Reply(data));
        WebSocketHandler.sendMessage(new RequestInstanceRefresh());
    }
    
    public static class Reply extends BaseData {
        public boolean success;

        public Reply(BaseData data) {
            this.requestId = data.requestId;
            this.success = true;
        }
    }
}
