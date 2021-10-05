package net.creeperhost.creeperlauncher.os.platform;

import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.os.platform.window.IWindowHelper;
import net.creeperhost.creeperlauncher.util.FileUtils;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the Linux platform.
 * <p>
 * Created by covers1624 on 9/2/21.
 */
public class LinuxPlatform extends UnixPlatform {

    private static final String LAUNCHER_URL = "https://launcher.mojang.com/download/Minecraft.tar.gz";
    private static final String LAUNCHER_EXECUTABLE = "minecraft-launcher/minecraft-launcher";

    public LinuxPlatform() {
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

    @Override
    public void unpackLauncher(Path downloadedLauncher) throws IOException {
        FileUtils.unTar(new GzipCompressorInputStream(Files.newInputStream(downloadedLauncher)), Constants.BIN_LOCATION);
        chmod755(getLauncherExecutable());

        //Due to a bug with the vanilla launcher it needs to be started without --workdir on the first start
        CompletableFuture.runAsync(() ->
        {
            try
            {
                List<String> args = new ArrayList<>();
                args.add(getLauncherExecutable().toAbsolutePath().toString());

                ProcessBuilder builder = new ProcessBuilder(args);
                Process process = builder.start();
                Thread.sleep(1000);
                process.destroyForcibly();
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }).join();
    }
}
