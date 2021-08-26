package net.creeperhost.creeperlauncher.os.platform;

import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.os.platform.window.win.WindowsWindowHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Represents the Windows Platform.
 * <p>
 * Created by covers1624 on 9/2/21.
 */
public class WindowsPlatform extends BasePlatform {

    private static final String LAUNCHER_URL = "https://launcher.mojang.com/download/Minecraft.exe";
    private static final String LAUNCHER_EXECUTABLE = "launcher.exe";

    public WindowsPlatform() {
        super(new WindowsWindowHelper());
    }

    @Override
    public String getLauncherURL() {
        return LAUNCHER_URL;
    }

    @Override
    public Path getLauncherExecutable() {
        return Constants.BIN_LOCATION.resolve(LAUNCHER_EXECUTABLE);
    }

    @Override
    public void unpackLauncher(Path downloadedLauncher) throws IOException {
        Files.copy(downloadedLauncher, getLauncherExecutable(), StandardCopyOption.REPLACE_EXISTING);
    }
}
