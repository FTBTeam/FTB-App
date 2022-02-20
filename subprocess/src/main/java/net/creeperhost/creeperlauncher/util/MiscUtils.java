package net.creeperhost.creeperlauncher.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MiscUtils {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final DateFormat ISO_8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    public static CompletableFuture<?> allFutures(ArrayList<CompletableFuture<?>> futures) {
        CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])).exceptionally((t) ->
                {
                    LOGGER.warn("Future failed.", t);
                    return null;
                }
        );
        futures.forEach((x) ->
        {
            x.exceptionally((t) ->
            {
                combinedFuture.completeExceptionally(t);
                return null;
            });
        });
        return combinedFuture;
    }

    /**
     * Gets a random free Ephemeral port.
     *
     * @return The port. <code>-1</code> if no port could be found.
     */
    public static int getRandomEphemeralPort() {
        int tries = 0;
        while (tries < 5) {
            try (ServerSocket socket = new ServerSocket(0)) {
                return socket.getLocalPort();
            } catch (IOException ignored) {
                tries++;
            }
        }
        return -1;
    }

    /**
     * Split the provided string on space, respecting paired quotes.
     *
     * @param s The string to split.
     * @return The split list of arguments.
     */
    public static List<String> splitCommand(String s) {
        List<String> args = new LinkedList<>();
        boolean qt = false;
        StringBuilder builder = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '"') {
                qt = !qt;
                continue;
            }
            if (c == ' ' && !qt) {
                if (builder.isEmpty()) continue;
                args.add(builder.toString());
                builder.delete(0, builder.length());
                continue;
            }
            builder.append(c);
        }
        if (!builder.isEmpty()) {
            args.add(builder.toString());
        }
        return args;
    }

    public static long unixtime() {
        return System.currentTimeMillis() / 1000L;
    }

    public static String encodeURL(String urlStr) {
        if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) return urlStr;
        try {
            URL url = new URL(urlStr);
            URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
            return uri.toString();
        } catch (MalformedURLException | URISyntaxException ex) {
            LOGGER.warn("Failed to re-encode URL.", ex);
            return urlStr;
        }
    }

    /**
     * For some reason Minecraft like to give us UUID's without the hyphens...
     *
     * This adds them back, if we get a string with hyphens, it returns the same string.
     */
    public static UUID createUuidFromStringWithoutDashes(String uuid) {
        if (uuid.contains("-")) {
            return UUID.fromString(uuid);
        }

        String uuidWithoutDashes = uuid.replaceFirst("([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)", "$1-$2-$3-$4-$5");
        return UUID.fromString(uuidWithoutDashes);
    }
}
