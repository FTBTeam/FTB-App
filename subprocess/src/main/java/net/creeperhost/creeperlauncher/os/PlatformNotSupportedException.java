package net.creeperhost.creeperlauncher.os;

/**
 * May be thrown when an operation was performed that is currently not supported on this platform.
 * These errors are almost always fatal.
 * <p>
 * Created by covers1624 on 12/2/21.
 */
public class PlatformNotSupportedException extends RuntimeException {

    public PlatformNotSupportedException() {
        super(System.getProperty("os.name"));
    }
}
