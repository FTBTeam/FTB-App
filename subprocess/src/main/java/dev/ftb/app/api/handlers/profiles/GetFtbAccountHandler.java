package dev.ftb.app.api.handlers.profiles;

import com.google.gson.Gson;
import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.data.PrivateBaseData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;
import dev.ftb.app.services.storage.VaultException;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetFtbAccountHandler implements IMessageHandler<BaseData> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetFtbAccountHandler.class);
    
    @Override
    public void handle(MessageContext context, BaseData data) {
        String accountData;
        try {
            accountData = context.services().storage().vault().get("ftbAccount");
        } catch (VaultException e) {
            LOGGER.error("Failed to retrieve FTB account data from vault", e);
            context.reply(new Reply(data, null));
            return;
        }

        // Deserialize the account data
        StoreFtbAccountHandler.CompleteTokenData authData = new Gson().fromJson(accountData, StoreFtbAccountHandler.CompleteTokenData.class);
        if (authData == null) {
            context.reply(new Reply(data, null));
            return;
        }
        
        context.reply(new Reply(data, authData));
    }
    
    public static class Reply extends PrivateBaseData {
        public boolean success;
        public StoreFtbAccountHandler.@Nullable CompleteTokenData authData;
        
        public Reply(BaseData data, StoreFtbAccountHandler.@Nullable CompleteTokenData authData) {
            this.type = data.type + "Reply";
            this.requestId = data.requestId;
            this.success = authData != null;
            this.authData = authData;
        }
    }
}
