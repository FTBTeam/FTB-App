package net.creeperhost.creeperlauncher.os.platform;

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

    public static void chmod755(Path path) throws IOException {
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxr-xr-x"));
    }

}
