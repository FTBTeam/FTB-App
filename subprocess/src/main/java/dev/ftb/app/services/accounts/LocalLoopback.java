package dev.ftb.app.services.accounts;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.ftb.app.util.Result;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/// Local loopback implementation for handling the exchange of OAuth tokens outside the normal web-based flow.
/// The port is randomly allocated and the url can be provided via the [redirectUri] call
/// 
/// Intended use:
/// - Start the local server
/// - Get the url
/// - Send the user to the login page with the redirect url
/// - Handle the response and pass back to the initiator
/// 
/// TODO: In the future, this could be used for other things so we could make it less specific to the oauth code flow. 
public final class LocalLoopback implements Closeable {    
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalLoopback.class);
    private static final String CALLBACK_PATH = "/callback";

    private final HttpServer server;
    private final String expectedState;
    private final CompletableFuture<Callback> result = new CompletableFuture<>();

    private LocalLoopback(HttpServer server, String expectedState, Duration timeout) {
        this.server = server;
        this.expectedState = expectedState;

        server.createContext("/", this::handle);
        server.start();
        
        // When the loop is completed, we call back and stop the server if it hasn't been stopped yet.
        result.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
            .whenCompleteAsync((callback, error) -> server.stop(0));
    }

    /// Starts listening. On failure the caller should fall back to the device code flow.
    ///
    /// @param expectedState the `state` sent in the authorize URL, the callback must echo it back exactly
    public static Result<LocalLoopback, String> start(String expectedState, Duration timeout) {
        if (expectedState.isBlank()) {
            return Result.err("A state value is required");
        }

        try {
            var server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            return Result.ok(new LocalLoopback(server, expectedState, timeout));
        } catch (IOException e) {
            LOGGER.warn("Failed to start the login loopback listener", e);
            return Result.err("Couldn't start the local login listener: " + e.getMessage());
        }
    }

    /// The `redirect_uri` to send in the authorize request, and again in the code exchange. They must match exactly.
    public String redirectUri() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + CALLBACK_PATH;
    }

    /// Completes once with the provider's response. Completes exceptionally with a `TimeoutException` if the user
    /// never finishes, or is cancelled by [#close()].
    public CompletableFuture<Callback> result() {
        return result;
    }

    private void handle(HttpExchange exchange) throws IOException {
        Callback callback;

        try (exchange) {
            // Reject anything that isn't what we're expecting.
            if (!CALLBACK_PATH.equals(exchange.getRequestURI().getPath())) {
                respond(exchange, 404, "Not found.");
                return;
            }

            if (!"GET".equals(exchange.getRequestMethod())) {
                respond(exchange, 405, "Method not allowed.");
                return;
            }

            if (result.isDone()) {
                respond(exchange, 410, "This login has already finished. You can close this tab.");
                return;
            }

            var query = parseQuery(exchange.getRequestURI().getRawQuery());
            if (!expectedState.equals(query.get("state"))) {
                LOGGER.warn("Ignoring a login callback with a missing or unexpected state");
                respond(exchange, 400, "This login link isn't valid. Return to the FTB App and try again.");
                return;
            }

            // We're either handed the exchange token or we're not.
            var code = query.get("code");
            var error = query.get("error");
            if (code == null || code.isBlank()) {
                callback = new Callback(null, error != null ? error : "invalid_request", query.get("error_description"));
                respond(exchange, 200, "Sign in didn't complete. You can close this tab and try again in the FTB App.");
            } else {
                callback = new Callback(code, null, null);
                respond(exchange, 200, "You're signed in. You can close this tab and return to the FTB App.");
            }
        }

        // Only after the exchange is closed, so the browser gets the full page before the server stops
        result.complete(callback);
    }

    /// `message` must be fixed text, never anything from the request.
    private static void respond(HttpExchange exchange, int status, String message) throws IOException {
        byte[] body = ("<!doctype html><meta charset=\"utf-8\"><title>FTB App</title>"
            + "<body style=\"font-family:system-ui,sans-serif;padding:2rem\"><p>" + message + "</p></body>")
            .getBytes(StandardCharsets.UTF_8);

        var headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "text/html; charset=utf-8");
        headers.set("Cache-Control", "no-store");
        headers.set("Referrer-Policy", "no-referrer");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
    }

    private static Map<String, String> parseQuery(@Nullable String raw) {
        var map = new HashMap<String, String>();
        if (raw == null || raw.isEmpty()) {
            return map;
        }

        for (var pair : raw.split("&")) {
            int split = pair.indexOf('=');
            if (split <= 0) {
                continue;
            }

            // First value wins, a repeated parameter can't override `state` or `code`
            map.putIfAbsent(
                URLDecoder.decode(pair.substring(0, split), StandardCharsets.UTF_8),
                URLDecoder.decode(pair.substring(split + 1), StandardCharsets.UTF_8)
            );
        }

        return map;
    }

    /// Stops listening. A pending [#result()] is cancelled. Safe to call more than once, the server is only stopped by
    /// the completion hook in the constructor.
    @Override
    public void close() {
        result.cancel(false);
    }

    /// The callback is either going to be an error, or a code.
    public record Callback(@Nullable String code, @Nullable String error, @Nullable String errorDescription) {
        public boolean isSuccess() {
            return code != null;
        }
    }
}
