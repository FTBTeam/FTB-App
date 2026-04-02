package dev.ftb.app.util;

import org.jspecify.annotations.Nullable;

import java.util.stream.Stream;

/**
 * Utility methods for working with streams.
 */
public class StreamUtils {

    /**
     * Returns the only element in the stream, or the default value if empty or multiple elements exist.
     *
     * @param stream       The stream to process
     * @param defaultValue The default value to return if stream is empty or has multiple elements
     * @param <T>          The type of elements
     * @return The single element, or default value
     */
    @Nullable
    public static <T> T onlyOrDefault(Stream<T> stream, @Nullable T defaultValue) {
        var iterator = stream.iterator();
        if (!iterator.hasNext()) {
            return defaultValue;
        }
        T value = iterator.next();
        if (iterator.hasNext()) {
            return defaultValue;
        }
        return value;
    }

    private StreamUtils() {
    }
}
