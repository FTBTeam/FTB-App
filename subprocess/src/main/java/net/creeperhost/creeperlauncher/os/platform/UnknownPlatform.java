package net.creeperhost.creeperlauncher.os.platform;

import net.creeperhost.creeperlauncher.os.Platform;
import net.creeperhost.creeperlauncher.os.platform.window.IWindowHelper;

import java.nio.file.Path;

/**
 * Represents an unsupported Platform.
 * All operations will result in an {@link UnsupportedOperationException} being thrown.
 * <p>
 * Created by covers1624 on 9/2/21.
 */
public class UnknownPlatform implements Platform {

    //@formatter:off
    @Override public IWindowHelper getWindowHelper() { throw new UnsupportedOperationException(); }
    @Override public String getLauncherURL() { throw new UnsupportedOperationException(); }
    @Override public Path getLauncherExecutable() { throw new UnsupportedOperationException(); }
    @Override public void unpackLauncher(Path downloadedLauncher) { throw new UnsupportedOperationException(); }
    @Override public boolean installLauncher() { throw new UnsupportedOperationException(); }
    @Override public ProcessBuilder buildLauncherProcess() { throw new UnsupportedOperationException(); }
    //@formatter:on
}
