package dev.ftb.app.util;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class MiscUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(MiscUtils.class);

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
    
    @Nullable
    public static UUID tryParseUuid(String uuid) {
        try {
            return UUID.fromString(uuid);
        } catch (Exception e) {
            LOGGER.warn("Failed to parse UUID from string '{}': {}", uuid, e.getMessage());
            // Invalid UUID format
            return null;
        }
    }
}
