package dev.ftb.app.api.handlers.profiles;

import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;
import dev.ftb.app.services.accounts.TokenExchange;
import dev.ftb.app.structs.accounts.AccountProvider;
import dev.ftb.app.structs.accounts.DeviceAuthResponse;
import org.jspecify.annotations.Nullable;

/// Handles the auth flow for a Minecraft account, passing over to [dev.ftb.app.services.accounts.minecraft.MicrosoftOAuthProcess]
/// bulk of the actual flow.
/// - Frontend pings to us for a initial device token
/// - Backend hands it back
/// - Frontend sends a poll ping every X seconds
/// - Frontend sends a cancel ping if something is closed.
/// - Backend replies with the auth flow stage.
public class DeviceAuthFlowHandler implements IMessageHandler<DeviceAuthFlowHandler.Data> {
    @Override
    public void handle(MessageContext context, Data data) {
        TokenExchange<?> exchange = data.provider == AccountProvider.FTB
            ? context.services().accounts().ftb().tokenExchange()
            : context.services().accounts().minecraft().tokenExchange();

        if (data.flowStage == FlowStage.GET_CODE) {
            var initiate = exchange.initiate();
            if (initiate.isErr()) {
                System.err.println("Failed to initiate: " + initiate.unwrapErr());
                return;
            }
    
            DeviceAuthResponse code = initiate.unwrap();
            // TODO: Return it.
        } else if (data.flowStage == FlowStage.CHECK_FOR_TOKEN) {
            if (data.deviceToken == null) {
                // TODO: Reply with error
                return;
            }
            
            var pollResult = exchange.poll(data.deviceToken);
            switch (pollResult) {
                case TokenExchange.PollState.Pending<?>(var slowDown, var retry, var detail) -> {
                    if (slowDown) {
                        // TODO: Tell the frontend to slow down!
                    }
                    System.out.printf("Pending (slowDown=%s, retry=%s, detail=%s)%n", slowDown, retry, detail);
                }
                case TokenExchange.PollState.Done<?>(var token) -> {
                    System.out.println("Got token: " + token);
                    // TODO: Complete - Store - Reply
                    // TODO: If this is Microsoft, we have to jump through the auth flow loops :cry:
                    // TODO: Forward back state updates via events.
                    return;
                }
                case TokenExchange.PollState.Failed<?>(var message, var detail) -> {
                    System.out.printf("Failed: %s (%s)%n", message, detail);
                    // TODO: Forward to the frontend
                    return;
                }
            }
            
        } else {
            // TODO: Support cancelling the flow.
        }
    }

    public static class Data extends BaseData {
        FlowStage flowStage;
        AccountProvider provider;
        
        @Nullable String deviceToken;
    }
    
    public static class Reply extends BaseData {
//        DevicePollResult<MicrosoftFinalToken> result;
    }
    
    enum FlowStage {
        GET_CODE,
        CHECK_FOR_TOKEN,
        CANCEL;
    }
}
