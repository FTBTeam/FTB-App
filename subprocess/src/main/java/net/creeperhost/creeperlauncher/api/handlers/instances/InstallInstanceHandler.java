package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.*;
import net.creeperhost.creeperlauncher.api.SimpleDownloadableFile;
import net.creeperhost.creeperlauncher.api.data.instances.InstallInstanceData;
import net.creeperhost.creeperlauncher.api.data.other.InstalledFileEventData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.install.tasks.FTBModPackInstallerTask;
import net.creeperhost.creeperlauncher.pack.ModPack;
import net.creeperhost.creeperlauncher.pack.LocalInstance;
import net.creeperhost.creeperlauncher.util.GsonUtils;
import net.creeperhost.creeperlauncher.util.MiscUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class InstallInstanceHandler implements IMessageHandler<InstallInstanceData>
{
    private static final Logger LOGGER = LogManager.getLogger();

    //TODO: Make these local instead of static once Exceptions work properly again
    public static AtomicBoolean hasError = new AtomicBoolean(false);
    public static AtomicReference<String> lastError = new AtomicReference<>();
    FTBModPackInstallerTask install;

    @Override
    public void handle(InstallInstanceData data)
    {
        LOGGER.debug("Received install pack message for " + "ID:" + data.id + " VERSION:" + data.version + " PACKTYPE:" + data.packType);
        hasError.set(false);
        if (CreeperLauncher.isInstalling.get())
        {
            Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "error", "Install in progress.", CreeperLauncher.currentInstall.get().currentUUID));
            return;
        }
        Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "init", "Install started.", ""));
        LocalInstance instance;
        if(data.uuid != null && data.uuid.length() > 0)
        {
            try {
                instance = new LocalInstance(Settings.getInstanceLocOr(Constants.INSTANCES_FOLDER_LOC).resolve(data.uuid));
                data.packType = instance.packType;
                ModPack pack = FTBModPackInstallerTask.getPackFromAPI(data.id, data.version, data._private, data.packType);
                if (pack == null) {
                    pack = FTBModPackInstallerTask.getPackFromAPI(data.id, data.version, !data._private, data.packType);
                }
                List<SimpleDownloadableFile> files = pack.getFiles();
                Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "files", GsonUtils.GSON.toJson(files), ""));
                install = instance.update(data.version);
            } catch (Exception exception) {
                exception.printStackTrace();
                lastError.set("Instance not found, aborting update.");
                hasError.set(true);
                if(CreeperLauncher.currentInstall.get() != null)
                    CreeperLauncher.currentInstall.get().cancel();
                Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "error", lastError.get(), data.uuid));
                return;
            }
        } else {
            ModPack pack = FTBModPackInstallerTask.getPackFromAPI(data.id, data.version, data._private, data.packType);
            if (pack == null) {
                pack = FTBModPackInstallerTask.getPackFromAPI(data.id, data.version, !data._private, data.packType);
            }
            List<SimpleDownloadableFile> files = pack.getFiles();
            Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "files", GsonUtils.GSON.toJson(files), ""));
            instance = new LocalInstance(pack, data.version, data._private, data.packType);
            data.uuid = instance.getUuid().toString();
            LOGGER.debug("Running install task");
            install = instance.install();
            Instances.addInstance(instance.getUuid(), instance);
        }
        install.currentTask.exceptionally((t) ->
        {
            if (t != null)
            {
                if (t instanceof CompletionException) t = t.getCause();
                String msg = t.getMessage();
                String[] split = msg.split("\n");
                if (split.length > 0)
                    msg = split[0];

                int indexOf = msg.indexOf(":");
                if (indexOf != -1)
                    msg = msg.substring(indexOf + 2);

                LOGGER.error("Error occurred whilst downloading pack:", t);
                lastError.set(msg);
                hasError.set(true);
                CreeperLauncher.currentInstall.get().cancel();
            }
            return null;
        });
        if (install != null)
        {
            double curProgress;
            long lastBytes = 0;
            long lastTime = System.currentTimeMillis() / 1000L;
            long lastSpeed = 0;
            int sinceLastChange = 0;
            long lastAverage = 0;
            double lastProgress = 0.00d;
            boolean sentHundred = false;
            while (((curProgress = install.getProgress()) <= 100) && (hasError.get() == false))
            {
                long time = System.currentTimeMillis() / 1000L;
                long speed = 0;
                long averageSpeed = 0;
                long curBytes = FTBModPackInstallerTask.currentBytes.get();
                if ((curBytes > 0) && ((time - lastTime) > 0))
                {
                    speed = ((curBytes - lastBytes) / (time - lastTime)) * 8;
                    long runtime = MiscUtils.unixtime() - FTBModPackInstallerTask.startTime.get();
                    averageSpeed = (curBytes / runtime) * 8;
                    if (averageSpeed == 0) averageSpeed = lastAverage;
                    if (speed == 0) speed = lastSpeed;
                    lastAverage = averageSpeed;
                } else
                {
                    speed = lastSpeed;
                }
                FTBModPackInstallerTask.currentSpeed.set(speed);
                FTBModPackInstallerTask.averageSpeed.set(averageSpeed);
                if (curProgress != lastProgress && FTBModPackInstallerTask.currentStage == FTBModPackInstallerTask.Stage.DOWNLOADS)
                {
                    sinceLastChange = 0;
                    Settings.webSocketAPI.sendMessage(new InstallInstanceData.Progress(data, curProgress, speed, curBytes, FTBModPackInstallerTask.overallBytes.get(), FTBModPackInstallerTask.Stage.DOWNLOADS));
                    Settings.webSocketAPI.sendMessage(new InstalledFileEventData.Reply(FTBModPackInstallerTask.batchedFiles));
                    FTBModPackInstallerTask.batchedFiles.clear();
                } else
                {
                    if (FTBModPackInstallerTask.currentStage != FTBModPackInstallerTask.Stage.DOWNLOADS)
                    {
                        Settings.webSocketAPI.sendMessage(new InstallInstanceData.Progress(data, 0.00d, speed, curBytes, -1, FTBModPackInstallerTask.currentStage));
                        curProgress = 0d;
                    }
                    if (curProgress > 0)
                    {
                        sinceLastChange = 0;
                        Settings.webSocketAPI.sendMessage(new InstallInstanceData.Progress(data, curProgress, speed, curBytes, FTBModPackInstallerTask.overallBytes.get(), FTBModPackInstallerTask.Stage.DOWNLOADS));
                        Settings.webSocketAPI.sendMessage(new InstalledFileEventData.Reply(FTBModPackInstallerTask.batchedFiles));
                        FTBModPackInstallerTask.batchedFiles.clear();
                    } else
                    {
                        if (FTBModPackInstallerTask.currentStage != FTBModPackInstallerTask.Stage.DOWNLOADS)
                        {
                            Settings.webSocketAPI.sendMessage(new InstallInstanceData.Progress(data, 0.00d, speed, curBytes, -1, FTBModPackInstallerTask.currentStage));
                            curProgress = 0d;
                        }
                        if (curProgress > 0)
                        {
                            ++sinceLastChange;
                        }
                    }
                }
                if (sinceLastChange > 120)
                {
                    lastError.set("No bytes transferred in 60 seconds, cancelling...");
                    hasError.set(true);
                    CreeperLauncher.currentInstall.get().cancel();
                    break;
                }
                //if(curProgress == 100)

                if(FTBModPackInstallerTask.currentStage == FTBModPackInstallerTask.Stage.POSTINSTALL)
                {
                    if (curProgress <= 100d && (!hasError.get()))
                    {
                        if(!sentHundred) {
                            //Make sure we always send 100%, fast internets cause issues.
                            Settings.webSocketAPI.sendMessage(new InstallInstanceData.Progress(data, 100d, averageSpeed, curBytes, FTBModPackInstallerTask.overallBytes.get(), FTBModPackInstallerTask.Stage.DOWNLOADS));
                            //Let them know we're now on POSTINSTALL
                            Settings.webSocketAPI.sendMessage(new InstallInstanceData.Progress(data, 100d, averageSpeed, curBytes, FTBModPackInstallerTask.overallBytes.get(), FTBModPackInstallerTask.Stage.POSTINSTALL));
                            sentHundred=true;
                        }
                    }
                }
                if (FTBModPackInstallerTask.currentStage == FTBModPackInstallerTask.Stage.FINISHED)
                {
                    if (curProgress <= 100d && (!hasError.get()))
                    {
                        Settings.webSocketAPI.sendMessage(new InstallInstanceData.Progress(data, 100d, averageSpeed, curBytes, FTBModPackInstallerTask.overallBytes.get(), FTBModPackInstallerTask.Stage.FINISHED));
                    }
                    break;
                }
                try
                {
                    Thread.sleep(500);
                } catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
                lastProgress = curProgress;
                lastTime = time;
                lastBytes = curBytes;
                lastSpeed = speed;
            }
            if (hasError.get())
            {
                Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "error", lastError.get(), instance.getUuid().toString()));
                CreeperLauncher.isInstalling.set(false);
            } else
            {
                Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "success", "Install complete.", instance.getUuid().toString()));
            }
        }
    }
}
