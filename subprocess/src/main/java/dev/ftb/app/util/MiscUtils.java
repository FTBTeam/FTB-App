package dev.ftb.app.util;

import net.covers1624.quack.util.SneakyUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
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

    public static <T, E extends Throwable> TimedResult<T> time(SneakyUtils.ThrowingSupplier<T, E> supplier) throws E {
        long start = System.nanoTime();
        T result = supplier.get();
        return new TimedResult<>(result, Duration.ofNanos(System.nanoTime() - start));
    }
    
    public static <E extends Throwable> Duration timeRun(SneakyUtils.ThrowingRunnable<E> runnable) throws E {
        long start = System.nanoTime();
        runnable.run();
        return Duration.ofNanos(System.nanoTime() - start);
    }

    public record TimedResult<T>(T result, Duration elapsed) {
        public String asString() {
            return formatDuration(elapsed);
        }
    }

    public static String formatDuration(Duration duration) {
        long nanos = duration.toNanos();
        if (nanos < 1_000L) return nanos + "ns";
        if (nanos < 1_000_000L) return String.format(Locale.ROOT, "%.2fµs", nanos / 1e3);
        if (nanos < 1_000_000_000L) return String.format(Locale.ROOT, "%.2fms", nanos / 1e6);
        if (nanos < 60_000_000_000L) return String.format(Locale.ROOT, "%.2fs", nanos / 1e9);

        long hours = duration.toHours();
        int minutes = duration.toMinutesPart();
        int seconds = duration.toSecondsPart();
        return hours > 0
            ? "%dh %dm %ds".formatted(hours, minutes, seconds)
            : "%dm %ds".formatted(minutes, seconds);
    }
}
