package net.creeperhost.creeperlauncher.os.platform;

import net.creeperhost.creeperlauncher.os.platform.window.IWindowHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

/**
 * Abstract class for Unix-like Platforms.
 * <p>
 * Created by covers1624 on 9/2/21.
 */
public abstract class UnixPlatform extends BasePlatform {

    protected UnixPlatform(IWindowHelper windowHelper) {
        super(windowHelper);
    }

    @Override
    protected void prepareLauncherEnvironment(ProcessBuilder builder) {
        super.prepareLauncherEnvironment(builder);
        //Thanks jikuja :D
        builder.environment().put("LC_ALL", "en_US.UTF-8");
    }

    public static void chmod755(Path path) throws IOException {
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxr-xr-x"));
    }

}
