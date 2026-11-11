package dev.ftb.app.api.handlers.profiles;

import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;
import dev.ftb.app.services.accounts.LocalLoopback;
import dev.ftb.app.util.Result;
import org.apache.commons.lang3.tuple.Pair;
import org.jspecify.annotations.Nullable;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class AuthFlowStartHandler implements IMessageHandler<AuthFlowStartHandler.Data> {
    private static final SecureRandom RANDOM = new SecureRandom();
    
    private final Map<String, Pair<LocalLoopback, String>> listeners = new HashMap<>();
    
    @Override
    public void handle(MessageContext context, Data data) {
        if (!data.handlerType.equals("ftb") && !data.handlerType.equals("microsoft")) {
            context.reply(Reply.error(data, "Incorrect handler type"));
            return;
        }
                
        var existing = listeners.get(data.handlerType);
        if (existing != null) {
            context.reply(Reply.success(data, existing.getRight()));
            return;
        }
        
        String state = randomUrlSafe(16);
        
        // If one doesn't exist, try and create it
        Result<LocalLoopback, String> loopbackResult = LocalLoopback.start(state, Duration.of(5, ChronoUnit.MINUTES));
        if (loopbackResult.isErr()) {
            context.reply(Reply.error(data, "Unable to loopbackResult the local server due to %s".formatted(loopbackResult.unwrapErr())));
            return;
        }

        LocalLoopback loopback = loopbackResult.unwrap();

        String verifier = randomUrlSafe(32);
        String challenge = base64Url(sha256(verifier.getBytes(StandardCharsets.US_ASCII)));
        String redirectUri = URLEncoder.encode(loopback.redirectUri().replace("127.0.0.1", "localhost"), StandardCharsets.UTF_8);
        
        listeners.put(data.handlerType, Pair.of(loopback, redirectUri));

        String url;
        if (data.handlerType.equals("ftb")) {
            url = "https://identity.feed-the-beast.com/realms/FTB/protocol/openid-connect/auth" +
                "?client_id=ftb-app" +
                "&response_type=code" +
                "&redirect_uri=" + redirectUri +
                "&scope=openid%20email%20profile%20offline_access" +
                "&state=" + state +
                "&code_challenge=" + challenge +
                "&code_challenge_method=S256";
        } else {
            url = "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize?client_id=f23e8ba8-f46b-41ed-b5c0-7994f2ebbbf8" +
                "&response_type=code" +
                "&redirect_uri=" + redirectUri +
                "&scope=offline_access%20XboxLive.signin" +
                "&state=" + state +
                "&code_challenge=" + challenge +
                "&code_challenge_method=S256" +
                "&prompt=select_account";
        }
        
        context.reply(Reply.success(data, url));
        
        // Don't block.
        loopback.result()
            .whenCompleteAsync((callback, throwable) -> {
               // Once it's complete, we need to handle it. It could be an issue.
                if (throwable != null) {
                    // TODO: Fire back an error;
                    throwable.printStackTrace();
                } else {
                    // Do something.
                    System.out.printf("Returned data of: %s", callback);
                }
                System.out.printf("callback: %s", callback);
                System.out.printf("throwable: %s", throwable);
                listeners.remove(data.handlerType);
                loopback.close(); 
            });
    }

    private static String randomUrlSafe(int bytes) {
        byte[] buf = new byte[bytes];
        RANDOM.nextBytes(buf);
        return base64Url(buf);
    }

    private static String base64Url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e); // every JVM has SHA-256
        }
    }

    public static class Data extends BaseData {
        public String handlerType;
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
