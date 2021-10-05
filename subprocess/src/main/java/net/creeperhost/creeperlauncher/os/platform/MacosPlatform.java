package net.creeperhost.creeperlauncher.os.platform;

import net.covers1624.quack.io.CopyingFileVisitor;
import net.covers1624.quack.io.IOUtils;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.os.platform.window.IWindowHelper;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Represents the MacOS platform.
 * <p>
 * Created by covers1624 on 9/2/21.
 */
public class MacosPlatform extends UnixPlatform {

    private static final String LAUNCHER_URL = "https://apps.modpacks.ch/FTB2/mac.zip";
    private static final String LAUNCHER_EXECUTABLE = "Minecraft.app/Contents/MacOS/launcher";
    private static final String[] ADDITIONAL_EXECUTABLES = {
            LAUNCHER_EXECUTABLE,
            "Minecraft.app/Contents/Minecraft Updater.app/Contents/MacOS/nativeUpdater"
    };

    public MacosPlatform() {
        super(IWindowHelper.NullWindowHelper.INSTANCE);
    }

    @Override
    public String getLauncherURL() {
        return LAUNCHER_URL;
    }

    @Override
    public Path getLauncherExecutable() {
        return Constants.BIN_LOCATION.resolve(LAUNCHER_EXECUTABLE);
    }

    public Path getLauncherOpenPath() {
        return Constants.BIN_LOCATION.resolve("Minecraft.app");
    }

    @Override
    public void unpackLauncher(Path downloadedLauncher) throws IOException {
        try (FileSystem fs = IOUtils.getJarFileSystem(downloadedLauncher, true)) {
            Path root = fs.getPath("/");
            Files.walkFileTree(root, new CopyingFileVisitor(root, Constants.BIN_LOCATION));
        }
        for (String executable : ADDITIONAL_EXECUTABLES) {
            //TODO, UnixPlatform.chmod755? Should be supported by MacOS?
            Constants.BIN_LOCATION.resolve(executable).toFile().setExecutable(true);
        }
    }

    @Override //Macos is speshal, this is validated with unit test.
    protected List<String> prepareLauncherProcessArgs() {
        List<String> args = super.prepareLauncherProcessArgs();
        args.add(0, "/usr/bin/open");
        args.set(1, getLauncherOpenPath().toAbsolutePath().toString());
        args.add(2, "--args");
        return args;
    }
}
