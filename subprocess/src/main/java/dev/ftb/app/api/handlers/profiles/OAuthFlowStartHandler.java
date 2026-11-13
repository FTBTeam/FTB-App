package dev.ftb.app.api.handlers.profiles;

import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;
import dev.ftb.app.services.accounts.LocalLoopback;
import dev.ftb.app.services.accounts.TokenExchange;
import dev.ftb.app.structs.accounts.AccountProvider;
import dev.ftb.app.structs.accounts.LoopbackHolder;
import dev.ftb.app.util.HashingUtils;
import dev.ftb.app.util.Result;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.concurrent.CancellationException;

public class OAuthFlowStartHandler implements IMessageHandler<OAuthFlowStartHandler.Data> {
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
            context.reply(Reply.error(data, "Unable to loopbackResult the local server due to %s".formatted(loopbackResult.unwrapErr())));
            return;
        }

        LocalLoopback loopback = loopbackResult.unwrap();

        String verifier = randomUrlSafe(32);
        String challenge = base64Url(HashingUtils.sha256(verifier.getBytes(StandardCharsets.US_ASCII)));
        var holder = new LoopbackHolder(loopback, state, verifier, loopback.redirectUri());
        listeners.put(data.provider, holder);
        
        String url = exchange.authorizeUrl(state, challenge, loopback.redirectUri(), TokenExchange.AccountPrompt.CHOOSE_ACCOUNT);
        
        // A success is the URL being returned, not anything to do with the loopback system.
        context.reply(Reply.success(data, url));
        
        // Don't block. Loopback will close once it finishes. Completion does blocking HTTP (the code exchange, and the
        // Xbox / Minecraft chain for Microsoft), so keep it off the common pool.
        loopback.result()
            .whenCompleteAsync((callback, throwable) -> {
                // Only remove our own entry, a newer login for this provider may have replaced it
                listeners.remove(data.provider, holder);
                
                if (throwable instanceof CancellationException) {
                    // Replaced by a newer login, or closed. Nothing to report.
                    return;
                }
                
                if (throwable != null) {
                    // TODO: Fire back an error; It'll be after the full reply so this will be an unsolicited response
                    throwable.printStackTrace();
                    return;
                }
                
                // We have a successful callback!
                if (data.provider == AccountProvider.FTB) {
                    // Exchange it for a real token + store. That's the flow.
                    // TODO: Store -> Reply
                } else {
                    // Exchange it for a real token, fire off the hellish microsoft flow.
                    // TODO: Exchange -> Run full MS to MC flow -> Store
                    // TODO: Use the same events that the device auth works for feedback.
                }
            }, context.services().threads().messageHandlerPool());
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
    }
    
    public static class Reply extends BaseData {
        @Nullable
        public final String url;
        public final boolean success;
        @Nullable public final String reason;

        public Reply(Data data, boolean success, @Nullable String url, @Nullable String reason) {
            this.type = data.type + "Reply";
            this.requestId = data.requestId;
            this.success = success;
            this.url = url;
            this.reason = reason;
        }
        
        public static Reply error(Data data, String error) {
            return new Reply(data, false, null, error);
        }
        
        public static Reply success(Data data, String url) {
            return new Reply(data, true, url, null);
        }
    }
}
