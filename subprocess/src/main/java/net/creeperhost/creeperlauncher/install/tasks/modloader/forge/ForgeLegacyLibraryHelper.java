package net.creeperhost.creeperlauncher.install.tasks.modloader.forge;

import com.google.common.hash.HashCode;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.quack.util.MultiHasher.HashFunc;
import net.creeperhost.creeperlauncher.install.OperationProgressTracker;
import net.creeperhost.creeperlauncher.install.ProgressTracker;
import net.creeperhost.creeperlauncher.install.tasks.DownloadTask;
import net.creeperhost.creeperlauncher.install.tasks.DownloadTask.DownloadValidation;
import net.creeperhost.creeperlauncher.install.tasks.ParallelTaskHelper;
import net.creeperhost.creeperlauncher.install.tasks.Task;
import net.creeperhost.creeperlauncher.install.tasks.TaskProgressAggregator;
import net.creeperhost.creeperlauncher.util.CancellationToken;
import net.creeperhost.creeperlauncher.pack.Instance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

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

    public static void installLegacyLibs(CancellationToken cancelToken, ProgressTracker tracker, Instance instance, String mcVersion) {
        List<DownloadTask> dlTasks = getLibraryTasks(instance, mcVersion);
        if (dlTasks.isEmpty()) return;

        LOGGER.info("Installing Forge Legacy libraries.");

        tracker.setDynamicStepCount(dlTasks.size());

        long totalSize = FastStream.of(dlTasks).longSum(DownloadTask::getSizeEstimate);
        List<Task> tasks = FastStream.of(dlTasks)
                .map(e -> e.wrapStepComplete(tracker))
                .toList();

        tracker.setDynamicStepCount(tasks.size());
        TaskProgressAggregator.aggregate(tracker.dynamicListener(), totalSize, l -> {
            ParallelTaskHelper.executeInParallel(cancelToken, Task.TASK_POOL, tasks, l);
        });
    }

    public static List<DownloadTask> getLibraryTasks(Instance instance, String mcVersion) {
        Path libsDir = instance.getDir().resolve("lib");
        List<DownloadTask> libraries = new LinkedList<>();
        switch (mcVersion) {
            case "1.4.7":
                addTask(libraries, libsDir.resolve("bcprov-jdk15on-147.jar"),
                        "https://maven.creeperhost.net/org/bouncycastle/bcprov-jdk15on/1.47/bcprov-jdk15on-1.47.jar",
                        1997327,
                        "b6f5d9926b0afbde9f4dbe3db88c5247be7794bb"
                );
            case "1.4.2":
                addTask(libraries, libsDir.resolve("argo-2.25.jar"),
                        "https://maven.creeperhost.net/net/sourceforge/argo/argo/2.25/argo-2.25.jar",
                        123642,
                        "bb672829fde76cb163004752b86b0484bd0a7f4b"
                );
                addTask(libraries, libsDir.resolve("guava-12.0.1.jar"),
                        "https://maven.creeperhost.net/com/google/guava/guava/12.0.1/guava-12.0.1.jar",
                        1795932,
                        "b8e78b9af7bf45900e14c6f958486b6ca682195f"
                );
                addTask(libraries, libsDir.resolve("asm-all-4.0.jar"),
                        "https://maven.creeperhost.net/org/ow2/asm/asm-all/4.0/asm-all-4.0-fml.jar",
                        212767,
                        "98308890597acb64047f7e896638e0d98753ae82"
                );
                break;
            case "1.5.2":
                addTask(libraries, libsDir.resolve("argo-small-3.2.jar"),
                        "https://maven.creeperhost.net/net/sourceforge/argo/argo/3.2/argo-3.2-small.jar",
                        91333,
                        "58912ea2858d168c50781f956fa5b59f0f7c6b51"
                );
                addTask(libraries, libsDir.resolve("guava-14.0-rc3.jar"),
                        "https://maven.creeperhost.net/com/google/guava/guava/14.0-rc3/guava-14.0-rc3.jar",
                        2189140,
                        "931ae21fa8014c3ce686aaa621eae565fefb1a6a"
                );
                addTask(libraries, libsDir.resolve("asm-all-4.1.jar"),
                        "https://maven.creeperhost.net/org/ow2/asm/asm-all/4.1/asm-all-4.1.jar",
                        214592,
                        "054986e962b88d8660ae4566475658469595ef58"
                );
                addTask(libraries, libsDir.resolve("deobfuscation_data_1.5.2.zip"),
                        "https://maven.creeperhost.net/cpw/mods/fml/deobfuscation_data/1.5.2/deobfuscation_data-1.5.2.zip",
                        201404,
                        "446e55cd986582c70fcf12cb27bc00114c5adfd9"
                );
                addTask(libraries, libsDir.resolve("scala-library-2.10.0.jar"),
                        "https://maven.creeperhost.net/org/scala-lang/scala-library/2.10.0/scala-library-2.10.0.jar",
                        7114639,
                        "43c6d98b445187c6b459a582c774ffb025120ef4"
                );
                break;
        }
        return libraries;
    }

    private static void addTask(List<DownloadTask> libraries, Path dest, String url, long size, String sha1) {
        DownloadTask task = DownloadTask.builder()
                .url(url)
                .dest(dest)
                .withValidation(DownloadValidation.of()
                        .withExpectedSize(size)
                        .withHash(HashFunc.SHA1, HashCode.fromString(sha1))
                )
                .build();
        if (!task.isRedundant()) {
            libraries.add(task);
        }
    }
}
