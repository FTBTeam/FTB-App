package dev.ftb.app.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;

public interface DebugTools extends Closeable {

    Logger LOGGER = LogManager.getLogger();
    String CLASS_NAME = "dev.ftb.app.debug.DebugToolsGui";
    boolean IS_AVAILABLE = isAvailable();
    None NONE = new DebugTools.None();

    default boolean isEnabled() {
        return true;
    }

    @Override
    void close();

    @Nullable
    static DebugTools load() {
        try {
            return (DebugTools) Class.forName(CLASS_NAME).getConstructor().newInstance();
        } catch (Throwable ex) {
            LOGGER.error("Failed to load DebugTools.", ex);
            return NONE;
        }
    }

    private static boolean isAvailable() {
        try {
            Class.forName(CLASS_NAME);
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }

    // @formatter:off
    class None implements DebugTools {
        @Override public boolean isEnabled() { return false; }
        @Override public void close() { }
    }
    // @formatter:on
}
