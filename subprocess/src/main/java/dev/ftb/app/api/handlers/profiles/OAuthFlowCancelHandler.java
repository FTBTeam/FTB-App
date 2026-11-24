package dev.ftb.app.api.handlers.profiles;

import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;
import dev.ftb.app.structs.accounts.AccountProvider;

public class OAuthFlowCancelHandler implements IMessageHandler<OAuthFlowCancelHandler.Data> {
    @Override
    public void handle(MessageContext context, Data data) {
        var listeners = context.services().accounts().loopbacks();

        var holder = listeners.get(data.provider);
        if (holder != null && holder.state().equals(data.flowId) && listeners.remove(data.provider, holder)) {
            // Removing it first means the completion ignores a callback that's already arrived
            holder.loopback().close();
        }

        context.reply(new Reply(data));
    }

    public static class Data extends BaseData {
        public AccountProvider provider;
        public String flowId;
    }

    public static class Reply extends BaseData {
        public Reply(Data data) {
            BaseData.attachReply(data, this);
        }
    }
}
