package net.creeperhost.creeperlauncher.os.platform.window;

import java.util.Collections;
import java.util.List;

/**
 * Represents a Window Helper for platform specific window interaction.
 */
public interface IWindowHelper {

    /**
     * Gets All windows available.
     *
     * @return The Windows.
     */
    default List<IWindow> getWindows() {
        return getWindows(-1);
    }

    /**
     * Gets all the windows owned by a specific Process ID.
     *
     * @param pid The Process ID to get windows for.
     * @return The Windows.
     */
    List<IWindow> getWindows(int pid);

    /**
     * Sets the current Screen position for the mouse cursor.
     *
     * @param x The X position.
     * @param y The Y position.
     */
    void setCursorPos(int x, int y);

    /**
     * If this Window Helper is functional or a stub.
     *
     * @return If the Window Helper is functional.
     */
    boolean isSupported();

    /**
     * Basic Null/NoOp implementation of {@link IWindowHelper}
     */
    class NullWindowHelper implements IWindowHelper {

        public static final IWindowHelper INSTANCE = new NullWindowHelper();

        private NullWindowHelper() {
        }

        //@formatter:off
        @Override public List<IWindow> getWindows(int pid) { return Collections.emptyList(); }
        @Override public void setCursorPos(int x, int y) { }
        @Override public boolean isSupported() { return false; }
        //@formatter:on
    }

}
