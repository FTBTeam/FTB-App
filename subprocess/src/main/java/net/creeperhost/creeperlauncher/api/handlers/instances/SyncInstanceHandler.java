package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.*;
import net.creeperhost.creeperlauncher.api.WebSocketAPI;
import net.creeperhost.creeperlauncher.api.data.other.CloseModalData;
import net.creeperhost.creeperlauncher.api.data.instances.InstallInstanceData;
import net.creeperhost.creeperlauncher.api.data.other.OpenModalData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.minetogether.lib.cloudsaves.CloudSaveManager;
import net.creeperhost.creeperlauncher.minecraft.McUtils;
import net.creeperhost.creeperlauncher.minecraft.modloader.ModLoader;
import net.creeperhost.creeperlauncher.minecraft.modloader.ModLoaderManager;
import net.creeperhost.creeperlauncher.pack.LocalInstance;
import net.creeperhost.creeperlauncher.util.FileUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class SyncInstanceHandler implements IMessageHandler<InstallInstanceData>
{
    public static AtomicReference<String> lastError = new AtomicReference<String>();

    @Override
    public void handle(InstallInstanceData data)
    {
        if (true) {
            Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "error", "Cloud saves are currently disabled!", ""));
            throw new UnsupportedOperationException();
        }
        if(data.uuid != null && data.uuid.length() > 0) {
//            if (CreeperLauncher.isInstalling) {
//                assert CreeperLauncher.currentInstall != null;
//                Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "error", "Install in progress.", CreeperLauncher.currentInstall.getInstance().getUuid().toString()));
//                return;
//            }
//            CreeperLauncher.isInstalling = true;
            Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "init", "Install started.", data.uuid));
            //Create the folder
            Path instanceDir = Constants.INSTANCES_FOLDER_LOC.resolve(data.uuid);
            Path instanceJson = instanceDir.resolve("instance.json");
            FileUtils.createDirectories(instanceDir);

            //Download the instance.json from the s3Bucket
            try {
                CloudSaveManager.downloadFile(data.uuid + "/instance.json", instanceJson, true, null);
            } catch (Exception ignored) {
                WebSocketAPI.LOGGER.error("Failed to download instance data from cloud saves");
                return;
            }

            LocalInstance instance;
            try {
                instance = new LocalInstance(instanceDir, instanceJson);
                instance.cloudSync(true);

                List<ModLoader> modLoaders = ModLoaderManager.getModLoaders(McUtils.getTargets(instance.getDir()));
                if (modLoaders.size() != 1) {
                    throw new RuntimeException("Only one mod loader is currently supported!");
                } else {
                    CompletableFuture.runAsync(() ->
                    {
                        OpenModalData.openModal("Preparing environment", "Installing Mod Loaders <br>", List.of());
                        ModLoader modLoader = modLoaders.get(0);
                        modLoader.install(instance);
                    }).thenRun(() ->
                    {
                        Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "success", "Install complete.", data.uuid));
                        Settings.webSocketAPI.sendMessage(new CloseModalData());
                    });
                }
            } catch (IOException e) {
                Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "error", lastError.get(), data.uuid));
                e.printStackTrace();
            }
        }
    }
}
