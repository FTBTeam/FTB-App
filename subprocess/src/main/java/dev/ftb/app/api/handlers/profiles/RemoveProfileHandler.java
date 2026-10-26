package dev.ftb.app.api.handlers.profiles;

import dev.ftb.app.accounts.AccountManager;
import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;

import java.util.UUID;

public class RemoveProfileHandler implements IMessageHandler<RemoveProfileHandler.Data> {
    @Override
    public void handle(MessageContext context, Data data) {
        boolean removed = false;
        
        // TODO: this could be much more graceful.
        AccountManager accounts = context.services().accounts();
        accounts.removeProfile(data.uuid);
        
        var foundProfile = accounts.getProfiles().stream()
            .filter(p -> p.getUuid().equals(data.uuid))
            .findFirst();
        
        if (foundProfile.isEmpty()) {
            removed = true;
        }
            
        context.reply(new Reply(data, removed));
    }

    public static class Data extends BaseData {
        UUID uuid;
    }

    private static class Reply extends Data {
        boolean success;

        public Reply(Data data, boolean success) {
            this.requestId = data.requestId;
            this.type = data.type + "Reply";
            this.success = success;
        }
    }
}
