package dev.ftb.app.api.handlers.profiles;

import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.data.other.RequestInstanceRefresh;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;
import dev.ftb.app.services.storage.VaultException;
import dev.ftb.app.util.ModpackApiUtils;
import org.slf4j.Logger;

public class SignOutFTBAccountHandler implements IMessageHandler<BaseData> {
    private final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SignOutFTBAccountHandler.class);
    
    @Override
    public void handle(MessageContext context, BaseData data) {
        // Not fatal, but not ideal.
        try {
            context.services().storage().vault().remove("ftbAccount");
        } catch (VaultException e) {
            LOGGER.error("Failed to remove FTB account from vault", e);
        }
        
        ModpackApiUtils.API_TOKEN = null;
        context.reply(new Reply(data));
        context.reply(new RequestInstanceRefresh());
    }
    
    public static class Reply extends BaseData {
        public boolean success;

        public Reply(BaseData data) {
            this.requestId = data.requestId;
            this.success = true;
        }
    }
}
