package dev.ftb.app.api.handlers.profiles;

import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;

/// Handles the auth flow for a Minecraft account, passing over to [dev.ftb.app.services.accounts.minecraft.MicrosoftOAuthProcess]
/// bulk of the actual flow.
public class MinecraftAuthFlowHandler implements IMessageHandler<MinecraftAuthFlowHandler.Data> {
    @Override
    public void handle(MessageContext context, Data data) {
        
    }

    public static class Data extends BaseData {
        FlowType type;
    }
    
    public static class Reply extends BaseData {
//        DevicePollResult<MicrosoftFinalToken> result;
    }
    
    enum FlowType {
        GET_CODE,
        CHECK_FOR_TOKEN,
        AUTHORIZE,
    }
}
