package net.creeperhost.creeperlauncher.os;

public enum OS {
    WIN,
    MAC,
    LINUX,
    UNKNOWN;

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
}
