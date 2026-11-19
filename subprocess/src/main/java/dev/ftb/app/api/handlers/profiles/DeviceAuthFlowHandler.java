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
                context.reply(Reply.error(data, initiate.unwrapErr()));
                return;
            }
    
            DeviceAuthResponse code = initiate.unwrap();
            context.reply(Reply.start(data, code));
        } else if (data.flowStage == FlowStage.CHECK_FOR_TOKEN) {
            if (data.deviceToken == null) {
                context.reply(Reply.error(data, "No device token provided!"));
                return;
            }
            
            var pollResult = exchange.poll(data.deviceToken);
            switch (pollResult) {
                case TokenExchange.PollState.Pending<?>(var slowDown, var retry, var detail) -> {
                    if (slowDown) {
                        context.reply(Reply.slowDown(data));
                        return;
                    }
                    System.out.printf("Pending (slowDown=%s, retry=%s, detail=%s)%n", slowDown, retry, detail);
                    context.reply(Reply.stillPending(data));
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
                    context.reply(Reply.error(data, message));
                }
            }
            
        } else {
            // TODO: How do we cancel? Do we call something? Or just clear out the loopback?
            
            // Just let it know it worked
            context.reply(Reply.justSuccess(data));
        }
    }

    public static class Data extends BaseData {
        FlowStage flowStage;
        AccountProvider provider;
        
        @Nullable String deviceToken;
    }
    
    public static class Reply extends BaseData {
        @Nullable DeviceAuthResponse start;
        @Nullable String reason;
        boolean success;
        boolean slowDown = false;
        boolean isPending = false;

        public Reply(Data data, @Nullable DeviceAuthResponse start, boolean success, @Nullable String reason, boolean slowDown, boolean isPending) {
            BaseData.attachReply(data, this);
            this.start = start;
            this.success = success;
            this.reason = reason;
            this.slowDown = slowDown;
            this.isPending = isPending;
        }
        
        public static Reply justSuccess(Data data) {
            return new Reply(data, null, true, null, false, false);
        }
        
        public static Reply stillPending(Data data) {
            return new Reply(data, null, true, null, false, true);
        }
        
        public static Reply error(Data data, @Nullable String reason) {
            return new Reply(data, null, false, reason, false, false);
        }
        
        public static Reply slowDown(Data data) {
            return new Reply(data, null, true, null, true, true);
        }
        
        public static Reply start(Data data, DeviceAuthResponse start) {
            return new Reply(data, start, true, null, false, false);
        }
    }
    
    enum FlowStage {
        GET_CODE,
        CHECK_FOR_TOKEN,
        CANCEL;
    }
}
