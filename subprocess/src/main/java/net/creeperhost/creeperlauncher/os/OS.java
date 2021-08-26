package net.creeperhost.creeperlauncher.os;

import net.creeperhost.creeperlauncher.os.platform.LinuxPlatform;
import net.creeperhost.creeperlauncher.os.platform.MacosPlatform;
import net.creeperhost.creeperlauncher.os.platform.UnknownPlatform;
import net.creeperhost.creeperlauncher.os.platform.WindowsPlatform;

import java.util.function.Supplier;

public enum OS {
    WIN(WindowsPlatform::new),
    MAC(MacosPlatform::new),
    LINUX(LinuxPlatform::new),
    UNKNOWN(UnknownPlatform::new);

    public static final OS CURRENT;

    static {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            CURRENT = OS.WIN;
        } else if (osName.contains("mac")) {
            CURRENT = OS.MAC;
        } else if (osName.contains("linux")) {
            CURRENT = OS.LINUX;
        } else {
            CURRENT = OS.UNKNOWN;
        }
    }

    private final Supplier<Platform> platformSupplier;
    private Platform platformImpl;

    OS(Supplier<Platform> platformSupplier) {
        this.platformSupplier = platformSupplier;
    }

    public Platform getPlatform() {
        if (this == UNKNOWN) {
            throw new PlatformNotSupportedException();
        }
        if (platformImpl == null) {
            platformImpl = platformSupplier.get();
        }
        return platformImpl;
    }
}
