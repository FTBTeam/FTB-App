package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.*;
import net.creeperhost.creeperlauncher.api.data.other.CloseModalData;
import net.creeperhost.creeperlauncher.api.data.instances.InstallInstanceData;
import net.creeperhost.creeperlauncher.api.data.other.OpenModalData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.minetogether.lib.cloudsaves.CloudSaveManager;
import net.creeperhost.creeperlauncher.minecraft.GameLauncher;
import net.creeperhost.creeperlauncher.minecraft.McUtils;
import net.creeperhost.creeperlauncher.minecraft.modloader.ModLoader;
import net.creeperhost.creeperlauncher.minecraft.modloader.ModLoaderManager;
import net.creeperhost.creeperlauncher.os.OS;
import net.creeperhost.creeperlauncher.os.Platform;
import net.creeperhost.creeperlauncher.pack.LocalInstance;
import net.creeperhost.creeperlauncher.util.FileUtils;

import java.io.FileNotFoundException;
import java.nio.file.Files;
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
        if(data.uuid != null && data.uuid.length() > 0) {
            if (CreeperLauncher.isInstalling.get()) {
                Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "error", "Install in progress.", CreeperLauncher.currentInstall.get().currentUUID));
                return;
            }
            Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "init", "Install started.", data.uuid));
            //Create the folder
            Path instanceDir = Constants.INSTANCES_FOLDER_LOC.resolve(data.uuid);
            Path instanceJson = instanceDir.resolve("instance.json");
            FileUtils.createDirectories(instanceDir);

            //Download the instance.json from the s3Bucket
            try {
                CloudSaveManager.downloadFile(data.uuid + "/instance.json", instanceJson, true, null);
            } catch (Exception ignored) {}

            LocalInstance instance;
            try {
                instance = new LocalInstance(instanceDir);
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
                        Platform platform = OS.CURRENT.getPlatform();
                        if (Files.notExists(platform.getLauncherExecutable())) {
                            OpenModalData.openModal("Preparing environment", "Installing Minecraft Launcher <br>", List.of());

                            platform.installLauncher();
                            if (!Files.exists(Constants.LAUNCHER_PROFILES_JSON)) GameLauncher.downloadLauncherProfiles();
                        }
                    }).thenRun(() ->
                    {
                        Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "success", "Install complete.", data.uuid));
                        Settings.webSocketAPI.sendMessage(new CloseModalData());
                    });
                }
            } catch (FileNotFoundException e) {
                Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "error", lastError.get(), data.uuid));
                e.printStackTrace();
            }
        }
    }
}
