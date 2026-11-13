package dev.ftb.app.services.accounts;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

public class LocalLoopbackTests {
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final String STATE = "expected-state";

    private static LocalLoopback start(Duration timeout) {
        return LocalLoopback.start(STATE, timeout).expect("Loopback failed to start");
    }

    private static int get(String url) throws IOException, InterruptedException {
        return CLIENT.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    @Test
    void redirectUriIsLoopbackWithARealPort() {
        try (var loopback = start(Duration.ofSeconds(10))) {
            var uri = URI.create(loopback.redirectUri());
            assertEquals("http", uri.getScheme());
            assertEquals("localhost", uri.getHost());
            assertTrue(uri.getPort() > 0);
            assertEquals("/callback", uri.getPath());
        }
    }

    @Test
    void listensOnTheIpv4Loopback() throws Exception {
        try (var loopback = start(Duration.ofSeconds(10))) {
            assertEquals(200, get(onHost(loopback.redirectUri(), "127.0.0.1") + "?code=v4&state=" + STATE));
            assertEquals("v4", loopback.result().get(5, TimeUnit.SECONDS).code());
        }
    }

    @Test
    void listensOnTheIpv6LoopbackOnTheSamePort() throws Exception {
        try (var loopback = start(Duration.ofSeconds(10))) {
            Assumptions.assumeTrue(loopback.listensOnIpv6(), "IPv6 loopback isn't available on this machine");

            assertEquals(200, get(onHost(loopback.redirectUri(), "[::1]") + "?code=v6&state=" + STATE));
            assertEquals("v6", loopback.result().get(5, TimeUnit.SECONDS).code());
        }
    }

    @Test
    void validCallbackCompletesWithTheCode() throws Exception {
        try (var loopback = start(Duration.ofSeconds(10))) {
            assertEquals(200, get(loopback.redirectUri() + "?code=abc.123-_%2B&state=" + STATE));

            var callback = loopback.result().get(5, TimeUnit.SECONDS);
            assertTrue(callback.isSuccess());
            assertEquals("abc.123-_+", callback.code());
        }
    }

    @Test
    void wrongOrMissingStateIsRejectedAndKeepsWaiting() throws Exception {
        try (var loopback = start(Duration.ofSeconds(10))) {
            assertEquals(400, get(loopback.redirectUri() + "?code=attacker&state=wrong"));
            assertEquals(400, get(loopback.redirectUri() + "?code=attacker"));
            assertFalse(loopback.result().isDone());

            assertEquals(200, get(loopback.redirectUri() + "?code=real&state=" + STATE));
            assertEquals("real", loopback.result().get(5, TimeUnit.SECONDS).code());
        }
    }

    @Test
    void repeatedParametersCantOverrideTheFirst() throws Exception {
        try (var loopback = start(Duration.ofSeconds(10))) {
            assertEquals(200, get(loopback.redirectUri() + "?state=" + STATE + "&code=first&code=second&state=other"));
            assertEquals("first", loopback.result().get(5, TimeUnit.SECONDS).code());
        }
    }

    @Test
    void providerErrorCompletesWithTheError() throws Exception {
        try (var loopback = start(Duration.ofSeconds(10))) {
            assertEquals(200, get(loopback.redirectUri() + "?error=access_denied&error_description=User+said+no&state=" + STATE));

            var callback = loopback.result().get(5, TimeUnit.SECONDS);
            assertFalse(callback.isSuccess());
            assertEquals("access_denied", callback.error());
            assertEquals("User said no", callback.errorDescription());
        }
    }

    @Test
    void otherPathsAndMethodsAreIgnored() throws Exception {
        try (var loopback = start(Duration.ofSeconds(10))) {
            var base = loopback.redirectUri().replace("/callback", "");
            assertEquals(404, get(base + "/favicon.ico"));

            var post = HttpRequest.newBuilder(URI.create(loopback.redirectUri() + "?code=x&state=" + STATE))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
            assertEquals(405, CLIENT.send(post, HttpResponse.BodyHandlers.discarding()).statusCode());

            assertFalse(loopback.result().isDone());
        }
    }

    @Test
    void timesOutAndStopsListening() throws Exception {
        var loopback = start(Duration.ofMillis(200));
        var redirect = loopback.redirectUri();

        var thrown = assertThrows(ExecutionException.class, () -> loopback.result().get(5, TimeUnit.SECONDS));
        assertInstanceOf(TimeoutException.class, thrown.getCause());

        assertStopsListening(redirect);
    }

    @Test
    void closeCancelsAndStopsListening() throws Exception {
        var loopback = start(Duration.ofSeconds(10));
        var redirect = loopback.redirectUri();

        loopback.close();
        loopback.close(); // second call is harmless

        assertThrows(CancellationException.class, () -> loopback.result().get(5, TimeUnit.SECONDS));
        assertStopsListening(redirect);
    }

    @Test
    void blankStateIsRefused() {
        assertTrue(LocalLoopback.start(" ", Duration.ofSeconds(1)).isErr());
    }

    private static String onHost(String redirect, String host) {
        return redirect.replace("localhost", host);
    }

    /// Stopping runs asynchronously after the result completes, so give it a moment. Checks both loopback addresses,
    /// connecting to one that was never bound is refused too, so this holds without IPv6.
    private static void assertStopsListening(String redirect) throws InterruptedException {
        for (var host : new String[]{"127.0.0.1", "[::1]"}) {
            assertStopsListening(redirect, host);
        }
    }

    private static void assertStopsListening(String redirect, String host) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            try {
                get(onHost(redirect, host) + "?state=" + STATE + "&code=late");
            } catch (IOException e) {
                return; // connection refused, server is gone
            }
            Thread.sleep(50);
        }

        fail("Loopback server on " + host + " was still listening after completing");
    }
}
