package dev.ftb.app.api.handlers.profiles;

import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.data.other.RequestInstanceRefresh;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;
import dev.ftb.app.util.ModpackApiUtils;

public class SignOutFTBAccountHandler implements IMessageHandler<BaseData> {
    @Override
    public void handle(MessageContext context, BaseData data) {
        context.services().storage().legacyCredentialStorage().remove("ftbAccount");
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
