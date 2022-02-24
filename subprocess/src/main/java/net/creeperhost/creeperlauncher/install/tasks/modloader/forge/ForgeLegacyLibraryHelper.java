package net.creeperhost.creeperlauncher.install.tasks.modloader.forge;

import com.google.common.hash.HashCode;
import net.covers1624.quack.util.MultiHasher.HashFunc;
import net.creeperhost.creeperlauncher.install.tasks.NewDownloadTask;
import net.creeperhost.creeperlauncher.install.tasks.NewDownloadTask.DownloadValidation;
import net.creeperhost.creeperlauncher.pack.LocalInstance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

/**
 * TODO, this is very much a hack, i'd like all these pushed up to the modpack manifests.
 * <p>
 * Created by covers1624 on 24/2/22.
 */
public class ForgeLegacyLibraryHelper {

    private static final Logger LOGGER = LogManager.getLogger();

    public static void installLegacyLibs(LocalInstance instance, String mcVersion) throws IOException {
        List<NewDownloadTask> tasks = getLibraryTasks(instance, mcVersion);
        if (tasks.isEmpty()) return;

        LOGGER.info("Installing Forge Legacy libraries.");

        for (NewDownloadTask task : tasks) {
            task.execute(null, null);
        }
    }

    public static List<NewDownloadTask> getLibraryTasks(LocalInstance instance, String mcVersion) {
        Path libsDir = instance.getDir().resolve("lib");
        List<NewDownloadTask> libraries = new LinkedList<>();
        switch (mcVersion) {
            case "1.4.7":
                libraries.add(makeTask(libsDir.resolve("bcprov-jdk15on-147.jar"),
                        "https://maven.creeperhost.net/org/bouncycastle/bcprov-jdk15on/1.47/bcprov-jdk15on-1.47.jar",
                        1997327,
                        "b6f5d9926b0afbde9f4dbe3db88c5247be7794bb"
                ));
            case "1.4.2":
                libraries.add(makeTask(libsDir.resolve("argo-2.25.jar"),
                        "https://maven.creeperhost.net/net/sourceforge/argo/argo/2.25/argo-2.25.jar",
                        123642,
                        "bb672829fde76cb163004752b86b0484bd0a7f4b"
                ));
                libraries.add(makeTask(libsDir.resolve("guava-12.0.1.jar"),
                        "https://maven.creeperhost.net/com/google/guava/guava/12.0.1/guava-12.0.1.jar",
                        1795932,
                        "b8e78b9af7bf45900e14c6f958486b6ca682195f"
                ));
                libraries.add(makeTask(libsDir.resolve("asm-all-4.0.jar"),
                        "https://maven.creeperhost.net/org/ow2/asm/asm-all/4.0/asm-all-4.0-fml.jar",
                        212767,
                        "98308890597acb64047f7e896638e0d98753ae82"
                ));
                break;
            case "1.5.2":
                libraries.add(makeTask(libsDir.resolve("argo-small-3.2.jar"),
                        "https://maven.creeperhost.net/net/sourceforge/argo/argo/3.2/argo-3.2-small.jar",
                        91333,
                        "58912ea2858d168c50781f956fa5b59f0f7c6b51"
                ));
                libraries.add(makeTask(libsDir.resolve("guava-14.0-rc3.jar"),
                        "https://maven.creeperhost.net/com/google/guava/guava/14.0-rc3/guava-14.0-rc3.jar",
                        2189140,
                        "931ae21fa8014c3ce686aaa621eae565fefb1a6a"
                ));
                libraries.add(makeTask(libsDir.resolve("asm-all-4.1.jar"),
                        "https://maven.creeperhost.net/org/ow2/asm/asm-all/4.1/asm-all-4.1.jar",
                        214592,
                        "054986e962b88d8660ae4566475658469595ef58"
                ));
                libraries.add(makeTask(libsDir.resolve("deobfuscation_data_1.5.2.zip"),
                        "https://maven.creeperhost.net/cpw/mods/fml/deobfuscation_data/1.5.2/deobfuscation_data-1.5.2.zip",
                        201404,
                        "446e55cd986582c70fcf12cb27bc00114c5adfd9"
                ));
                libraries.add(makeTask(libsDir.resolve("scala-library-2.10.0.jar"),
                        "https://maven.creeperhost.net/org/scala-lang/scala-library/2.10.0/scala-library-2.10.0.jar",
                        7114640,
                        "458d046151ad179c85429ed7420ffb1eaf6ddf85"
                ));
                break;
        }
        return libraries;
    }

    private static NewDownloadTask makeTask(Path dest, String url, long size, String sha1) {
        return NewDownloadTask.builder()
                .url(url)
                .dest(dest)
                .withValidation(DownloadValidation.of()
                        .withExpectedSize(size)
                        .withHash(HashFunc.SHA1, HashCode.fromString(sha1))
                )
                .build();
    }
}
