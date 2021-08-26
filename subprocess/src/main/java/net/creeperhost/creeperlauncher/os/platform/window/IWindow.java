package net.creeperhost.creeperlauncher.os.platform.window;

import java.awt.*;

/**
 * Represents a window on a specific monitor.
 */
public interface IWindow {

    /**
     * Gets a platform specific handle for interacting with this window.
     *
     * @return The handle.
     */
    Object getHandle();

    /**
     * Gets the title of this window.
     *
     * @return The title.
     */
    String getWindowTitle();

    /**
     * Gets the Process ID of the process who owns this window.
     *
     * @return Rhe PID.
     */
    int getPid();

    /**
     * Gets the size of this window.
     *
     * @return The size.
     */
    Rectangle getRect();

    /**
     * Gets the monitor this Window is located on.
     *
     * @return The window.
     */
    IMonitor getMonitor();

    /**
     * Gets the color of a specific pixel on the window.
     *
     * @param x The X pos
     * @param y The Y pos.
     * @return The color.
     */
    Color getPixelColour(int x, int y);

    /**
     * Triggers this window to be brought to the foreground and focussed.
     */
    void bringToFront();

    /**
     * If this Window is currently focused
     *
     * @return If this window has focus.
     */
    boolean hasFocus();

    /**
     * If this Window is currently in the foreground.
     *
     * @return If the Window is in the foreground.
     */
    boolean isForeground();
}
