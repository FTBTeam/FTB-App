package dev.ftb.app.api.handlers.profiles;

import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;
import dev.ftb.app.services.accounts.TokenExchange;
import dev.ftb.app.structs.accounts.AccountProvider;
import dev.ftb.app.structs.accounts.DeviceAuthResponse;
import dev.ftb.app.structs.accounts.DeviceFlow;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/// Handles the auth flow for a Minecraft account, passing over to [dev.ftb.app.services.accounts.minecraft.MicrosoftOAuthProcess]
/// bulk of the actual flow.
/// - Frontend pings to us for a initial device token
/// - Backend hands it back
/// - Frontend sends a poll ping every X seconds
/// - Frontend sends a cancel ping if something is closed.
/// - Backend replies with the auth flow stage.
/// - Backend starts to send events once the flow finishes for the auth step
public class DeviceAuthFlowHandler implements IMessageHandler<DeviceAuthFlowHandler.Data> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceAuthFlowHandler.class);

    @Override
    public void handle(MessageContext context, Data data) {
        switch (data.flowStage) {
            case GET_CODE -> this.start(context, data);
            case CHECK_FOR_TOKEN -> this.poll(context, data);
            case CANCEL -> {
                if (data.flowId != null) {
                    claim(context.services().accounts().deviceFlows(), data.provider, data.flowId);
                }
                context.reply(Reply.cancelled(data));
            }
        }
    }

    private void start(MessageContext context, Data data) {
        var initiate = exchange(context, data.provider).initiate();
        if (initiate.isErr()) {
            context.reply(Reply.failed(data, initiate.unwrapErr()));
            return;
        }

        DeviceAuthResponse response = initiate.unwrap();
        var flow = new DeviceFlow(
            UUID.randomUUID().toString(),
            response.deviceCode(),
            Instant.now().getEpochSecond() + response.expiresIn(),
            response.intervalOrDefault()
        );

        // Replaces any older flow for this provider, its polls then get cancelled
        context.services().accounts().deviceFlows().put(data.provider, flow);
        context.reply(Reply.codeIssued(data, flow, new DeviceUserCode(
            response.userCode(),
            response.verificationUri(),
            response.verificationUriComplete(),
            flow.expiresAt()
        )));
    }

    private void poll(MessageContext context, Data data) {
        var flows = context.services().accounts().deviceFlows();
        DeviceFlow flow = flows.get(data.provider);
        if (flow == null || data.flowId == null || !flow.flowId().equals(data.flowId)) {
            context.reply(Reply.cancelled(data));
            return;
        }

        if (Instant.now().getEpochSecond() >= flow.expiresAt()) {
            claim(flows, data.provider, flow.flowId());
            context.reply(Reply.failed(data, "The login code has expired. Please try again."));
            return;
        }

        switch (exchange(context, data.provider).poll(flow.deviceCode())) {
            case TokenExchange.PollState.Pending<?>(var slowDown, var retry, var detail) -> {
                if (slowDown) {
                    DeviceFlow slower = flows.computeIfPresent(data.provider, (k, current) -> current.flowId().equals(flow.flowId()) ? current.slowedDown() : current);
                    context.reply(Reply.pending(data, slower != null && slower.flowId().equals(flow.flowId()) ? slower.interval() : flow.interval()));
                    return;
                }

                if (retry) {
                    LOGGER.warn("{} device code poll failed, will retry: {}", data.provider, detail);
                }
                context.reply(Reply.pending(data, flow.interval()));
            }
            case TokenExchange.PollState.Done<?>(var token) -> {
                // Only the poll that removes the flow may finish it. If it was cancelled while this poll was waiting on
                // the provider, the user has closed the login, so the token is dropped.
                if (!claim(flows, data.provider, flow.flowId())) {
                    context.reply(Reply.cancelled(data));
                    return;
                }

                context.reply(Reply.complete(data));
                
                // Start finishing up the login process
                LoginFinisher.finish(context.services(), data.provider, flow.flowId(), token);
            }
            case TokenExchange.PollState.Failed<?>(var message, var detail) -> {
                LOGGER.error("{} device code login failed: {} ({})", data.provider, message, detail);
                claim(flows, data.provider, flow.flowId());
                context.reply(Reply.failed(data, message));
            }
        }
    }
    
    private static boolean claim(Map<AccountProvider, DeviceFlow> flows, AccountProvider provider, String flowId) {
        var removed = new AtomicBoolean(false);
        flows.computeIfPresent(provider, (k, current) -> {
            if (current.flowId().equals(flowId)) {
                removed.set(true);
                return null;
            }
            return current;
        });
        return removed.get();
    }

    private static TokenExchange<?> exchange(MessageContext context, AccountProvider provider) {
        return provider == AccountProvider.FTB
            ? context.services().accounts().ftb().tokenExchange()
            : context.services().accounts().minecraft().tokenExchange();
    }

    public static class Data extends BaseData {
        public FlowStage flowStage;
        public AccountProvider provider;
        public @Nullable String flowId;
    }

    public static class Reply extends BaseData {
        public final DeviceFlowStatus status;
        public final @Nullable String flowId;
        public final @Nullable DeviceUserCode code;
        public final int interval;
        public final @Nullable String reason;

        private Reply(Data data, DeviceFlowStatus status, @Nullable String flowId, @Nullable DeviceUserCode code, int interval, @Nullable String reason) {
            BaseData.attachReply(data, this);
            this.status = status;
            this.flowId = flowId;
            this.code = code;
            this.interval = interval;
            this.reason = reason;
        }

        static Reply codeIssued(Data data, DeviceFlow flow, DeviceUserCode code) {
            return new Reply(data, DeviceFlowStatus.CODE_ISSUED, flow.flowId(), code, flow.interval(), null);
        }

        static Reply pending(Data data, int interval) {
            return new Reply(data, DeviceFlowStatus.PENDING, null, null, interval, null);
        }

        static Reply complete(Data data) {
            return new Reply(data, DeviceFlowStatus.COMPLETE, null, null, 0, null);
        }

        static Reply failed(Data data, String reason) {
            return new Reply(data, DeviceFlowStatus.FAILED, null, null, 0, reason);
        }

        static Reply cancelled(Data data) {
            return new Reply(data, DeviceFlowStatus.CANCELLED, null, null, 0, null);
        }
    }
    
    public record DeviceUserCode(String userCode, String verificationUri, @Nullable String verificationUriComplete, long expiresAt) {}

    public enum DeviceFlowStatus {
        CODE_ISSUED,
        PENDING,
        COMPLETE,
        FAILED,
        CANCELLED
    }

    public enum FlowStage {
        GET_CODE,
        CHECK_FOR_TOKEN,
        CANCEL
    }
}
