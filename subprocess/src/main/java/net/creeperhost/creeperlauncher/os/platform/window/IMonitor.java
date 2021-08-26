package net.creeperhost.creeperlauncher.os.platform.window;

import java.awt.*;

/**
 * Represents a monitor.
 */
public interface IMonitor {

    /**
     * Gets this monitors index.
     *
     * @return The index.
     */
    int getNumber();

    /**
     * Gets the size of this monitor.
     *
     * @return The size.
     */
    Rectangle getBounds();
}
