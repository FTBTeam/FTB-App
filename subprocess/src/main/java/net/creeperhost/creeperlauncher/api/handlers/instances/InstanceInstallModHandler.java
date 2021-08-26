package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.DownloadableFile;
import net.creeperhost.creeperlauncher.api.data.instances.InstanceInstallModData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.install.tasks.DownloadTask;
import net.creeperhost.creeperlauncher.install.tasks.FTBModPackInstallerTask;
import net.creeperhost.creeperlauncher.install.tasks.http.IProgressUpdater;
import net.creeperhost.creeperlauncher.migration.migrators.DialogUtil;
import net.creeperhost.creeperlauncher.minecraft.McUtils;
import net.creeperhost.creeperlauncher.mod.Mod;
import net.creeperhost.creeperlauncher.pack.LocalInstance;
import net.creeperhost.creeperlauncher.util.LoaderTarget;
import net.creeperhost.creeperlauncher.util.MiscUtils;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class InstanceInstallModHandler implements IMessageHandler<InstanceInstallModData> {
    @Override
    public void handle(InstanceInstallModData data) {
        String _uuid = data.uuid;
        UUID uuid = UUID.fromString(_uuid);
        LocalInstance instance = Instances.getInstance(uuid);
        Mod mod = Mod.getFromAPI(data.modId);
        if (mod == null) {
            Settings.webSocketAPI.sendMessage(new InstanceInstallModData.Reply(data, "error", "Cannot find version "  + data.versionId, new ArrayList<>()));
            return;
        }

        Mod.Version version = mod.getVersion(data.versionId);
        if (version == null) {
            Settings.webSocketAPI.sendMessage(new InstanceInstallModData.Reply(data, "error", "Cannot find version " + data.versionId, new ArrayList<>()));
            return;
        }

        ArrayList<Mod.Version> filesToDownload = new ArrayList<>();
        filesToDownload.add(version);

        List<LoaderTarget> targets = McUtils.getTargets(instance.getDir());

        Optional<LoaderTarget> loaderTarget = targets.stream().filter(tar -> tar.getType().equals("modloader")).findFirst();
        Optional<LoaderTarget> minecraftTarget = targets.stream().filter(tar -> tar.getType().equals("game")).findFirst();

        List<Mod.Version> dependencies = new ArrayList<>();

        if (loaderTarget.isPresent() && minecraftTarget.isPresent()) {
            dependencies = version.getDependencies(
                    instance.getMods(),
                    dependencies,
                    loaderTarget.get(),
                    minecraftTarget.get()
            );

            if (!dependencies.isEmpty()) {
                StringBuilder dependString = new StringBuilder();
                for (Mod.Version dependency: dependencies) {
                    if(!dependency.existsOnDisk) {
                        dependString.append("<ul><b>").append(dependency.getParentMod().getName()).append(":</b> ").append(dependency.getName()).append("</ul>");
                    }
                }
                /*boolean confirm = DialogUtil.confirmDialog(
                    "Dependencies Required",
                    "Would you also like to install:<br><ul>" +
                    dependString +
                    "</ul><br>"
                );*/ boolean confirm = true;

                if (confirm) {
                    filesToDownload.addAll(dependencies.stream().filter((ver) -> !ver.existsOnDisk).collect(Collectors.toList()));
                }
            }
        }

        HashMap<DownloadableFile, ProgressTracker> fileTracker = new HashMap<>();
        ArrayList<DownloadTask> downloadTasks = new ArrayList<>();

        for(Mod.Version ver: filesToDownload) {
            DownloadableFile downloadableFile = ver.getDownloadableFile(instance);
            fileTracker.put(downloadableFile, new ProgressTracker(downloadableFile));
            DownloadTask downloadTask = new DownloadTask(downloadableFile, downloadableFile.getPath());
            downloadTasks.add(downloadTask);
        }

        List<Mod.Version> finalDependencies = dependencies;

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        long lastTime = System.currentTimeMillis() / 1000L;
        long lastSpeed = 0;
        long lastBytes = 0;
        final long[] lastAverage = {0};
        ScheduledFuture<?> progressTask = executor.scheduleAtFixedRate(() -> {
            long curBytes = 0;
            long totalBytes = 0;
            for(ProgressTracker progress: fileTracker.values()) {
                curBytes = progress.downloadedBytes.get();
                totalBytes = progress.file.getSize();
            }

            long time = System.currentTimeMillis() / 1000L;
            long speed = 0;
            long averageSpeed = 0;
            if ((curBytes > 0) && ((time - lastTime) > 0))
            {
                speed = ((curBytes - lastBytes) / (time - lastTime)) * 8;
                long runtime = MiscUtils.unixtime() - FTBModPackInstallerTask.startTime.get();
                averageSpeed = (curBytes / runtime) * 8;
                if (averageSpeed == 0) averageSpeed = lastAverage[0];
                if (speed == 0) speed = lastSpeed;
                lastAverage[0] = averageSpeed;
            } else
            {
                speed = lastSpeed;
            }

            Settings.webSocketAPI.sendMessage(
                    new InstanceInstallModData.Progress(
                            data,
                            (double) ((curBytes / totalBytes) * 100),
                            speed,
                            curBytes,
                            totalBytes
                    )
            );
        }, 0, 500, TimeUnit.MILLISECONDS);

        CompletableFuture.allOf(downloadTasks.stream().map(DownloadTask::execute).collect(Collectors.toList()).toArray(new CompletableFuture[downloadTasks.size()])).thenRunAsync(() ->
                {
                    progressTask.cancel(false);
                    executor.shutdown();
                    try {
                        instance.setModified(true);
                        instance.saveJson();
                    } catch(Exception ignored) {} //TODO: We REALLY need to stop ignoring these...
                    Settings.webSocketAPI.sendMessage(
                            new InstanceInstallModData.Reply(
                                    data,
                                    "success",
                                    "Downloaded successfully",
                                    finalDependencies
                            )
                    );
                }
            );
    }

    class ProgressTracker implements IProgressUpdater {

        final DownloadableFile file;

        public ProgressTracker(DownloadableFile file) {
            this.file = file;
        }

        volatile AtomicLong downloadedBytes = new AtomicLong(0);

        @Override
        public void update(long currentBytes, long delta, long totalBytes, boolean done) {
            downloadedBytes.addAndGet(delta);
        }

        public double getProgress() {
            return Math.max(((double)downloadedBytes.get() / (double)file.getSize()) * 100, 100);
        }
    }
}
