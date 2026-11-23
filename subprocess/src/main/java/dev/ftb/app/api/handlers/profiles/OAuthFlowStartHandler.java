package dev.ftb.app.api.handlers.profiles;

import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.data.accounts.LoginFlowEvent;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;
import dev.ftb.app.services.Services;
import dev.ftb.app.services.accounts.FtbAccount;
import dev.ftb.app.services.accounts.LocalLoopback;
import dev.ftb.app.services.accounts.TokenExchange;
import dev.ftb.app.structs.accounts.AccountProvider;
import dev.ftb.app.structs.accounts.LoopbackHolder;
import dev.ftb.app.util.HashingUtils;
import dev.ftb.app.util.Result;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

public class OAuthFlowStartHandler implements IMessageHandler<OAuthFlowStartHandler.Data> {
    private static final Logger LOGGER = LoggerFactory.getLogger(OAuthFlowStartHandler.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    
    @Override
    public void handle(MessageContext context, Data data) {         
        var listeners = context.services().accounts().loopbacks();
        
        // Each login has its own state and verifier, so a second request can't reuse the first one's URL. The user may
        // also have closed that tab, so start fresh. Closing cancels the old login, its completion below then ignores it.
        var existing = listeners.remove(data.provider);
        if (existing != null) {
            existing.loopback().close();
        }
        
        TokenExchange<?> exchange = data.provider == AccountProvider.FTB
            ? context.services().accounts().ftb().tokenExchange()
            : context.services().accounts().minecraft().tokenExchange();
        
        String state = randomUrlSafe(16);
        
        // If one doesn't exist, try and create it
        Result<LocalLoopback, String> loopbackResult = LocalLoopback.start(state, Duration.of(5, ChronoUnit.MINUTES));
        if (loopbackResult.isErr()) {
            context.reply(Reply.error(data, "Couldn't start the local sign in server (%s). Please try again.".formatted(loopbackResult.unwrapErr())));
            return;
        }

        LocalLoopback loopback = loopbackResult.unwrap();

        String verifier = randomUrlSafe(32);
        String challenge = base64Url(HashingUtils.sha256(verifier.getBytes(StandardCharsets.US_ASCII)));
        var holder = new LoopbackHolder(loopback, state, verifier, loopback.redirectUri());
        listeners.put(data.provider, holder);
        
        String url = data.accountId != null
            ? exchange.authorizeUrl(state, challenge, loopback.redirectUri(), TokenExchange.AccountPrompt.REUSE_SESSION, loginHint(context.services(), data.provider, data.accountId))
            : exchange.authorizeUrl(state, challenge, loopback.redirectUri(), TokenExchange.AccountPrompt.CHOOSE_ACCOUNT, null);
        
        // A success is the URL being returned
        context.reply(Reply.success(data, url, state));

        // Don't block. Loopback will close once it finishes. Completion does blocking HTTP (the code exchange, and the
        // Xbox / Minecraft chain for Microsoft), so keep it off the common pool.
        var services = context.services();
        loopback.result()
            .whenCompleteAsync((callback, throwable) -> {
                boolean ours = listeners.remove(data.provider, holder);

                Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null ? throwable.getCause() : throwable;
                if (!ours || cause instanceof CancellationException) {
                    return;
                }

                if (cause != null) {
                    if (cause instanceof TimeoutException) {
                        LoginFinisher.failed(services, data.provider, state, "The sign in took too long. Please try again.");
                    } else {
                        LOGGER.error("{} browser login failed", data.provider, cause);
                        LoginFinisher.failed(services, data.provider, state, "Something went wrong signing in. Please try again.");
                    }
                    return;
                }

                if (callback.code() == null) {
                    LOGGER.warn("{} returned an error to the login callback: {} ({})", data.provider, callback.error(), callback.errorDescription());
                    LoginFinisher.failed(services, data.provider, state, "access_denied".equals(callback.error())
                        ? "The sign in was cancelled in the browser."
                        : "Couldn't sign in (" + callback.error() + "). Please try again.");
                    return;
                }

                LoginFinisher.working(services, data.provider, state, LoginFlowEvent.LoginStep.EXCHANGING_CODE);
                var tokens = exchange.exchangeCodeForTokens(callback.code(), holder);
                if (tokens.isErr()) {
                    LoginFinisher.failed(services, data.provider, state, tokens.unwrapErr());
                    return;
                }

                LoginFinisher.finish(services, data.provider, state, tokens.unwrap());
            }, services.threads().messageHandlerPool());
    }
    
    private static @Nullable String loginHint(Services services, AccountProvider provider, String accountId) {
        if (provider == AccountProvider.FTB) {
            FtbAccount account = services.accounts().ftb().get(accountId);
            if (account == null) {
                return null;
            }

            return account.getEmail() != null ? account.getEmail() : account.getUsername();
        }

        // Minecraft profiles don't keep the Microsoft account's email, and the Minecraft name isn't a Microsoft login
        return null;
    }

    private static String randomUrlSafe(int bytes) {
        byte[] buf = new byte[bytes];
        RANDOM.nextBytes(buf);
        return base64Url(buf);
    }

    private static String base64Url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }
    
    public static class Data extends BaseData {
        public AccountProvider provider;
        /// Set when signing a known account back in. A UUID for Microsoft, the account's `sub` for FTB.
        public @Nullable String accountId;
    }
    
    public static class Reply extends BaseData {
        @Nullable
        public final String url;
        public final boolean success;
        @Nullable public final String reason;
        @Nullable public final String flowId;

        public Reply(Data data, boolean success, @Nullable String url, @Nullable String reason, @Nullable String flowId) {
            this.type = data.type + "Reply";
            this.requestId = data.requestId;
            this.success = success;
            this.url = url;
            this.reason = reason;
            this.flowId = flowId;
        }

        public static Reply error(Data data, String error) {
            return new Reply(data, false, null, error, null);
        }

        public static Reply success(Data data, String url, String flowId) {
            return new Reply(data, true, url, null, flowId);
        }
    }
}
