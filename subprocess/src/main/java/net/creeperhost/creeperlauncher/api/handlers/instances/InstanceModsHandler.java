package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.instances.InstanceModsData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.data.InstanceModifications;
import net.creeperhost.creeperlauncher.data.mod.CurseMetadata;
import net.creeperhost.creeperlauncher.data.mod.ModInfo;
import net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest;
import net.creeperhost.creeperlauncher.pack.Instance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class InstanceModsHandler implements IMessageHandler<InstanceModsData> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(InstanceModsData data) {
        var instance = Instances.getInstance(data.uuid);
        if (instance == null) return;

        var mods = instance.getMods();
        Settings.webSocketAPI.sendMessage(new InstanceModsData.Reply(data, mods));

        checkForUpdates(data, instance, mods);
    }

    private static void checkForUpdates(InstanceModsData data, Instance instance, List<ModInfo> mods) {
        var mcVersion = instance.versionManifest.getTargetVersion("game");

        InstanceModifications modifications = instance.getModifications();
        ModpackVersionManifest.Target ml = modifications != null ? modifications.getModLoaderOverride() : null;
        if (ml == null) {
            ml = instance.versionManifest.findTarget("modloader");
        }

        // Nothing to do, we don't know what mod loader or mc version this instance is.
        if (ml == null || mcVersion == null) {
            Settings.webSocketAPI.sendMessage(new InstanceModsData.UpdateCheckingFinished(data));
            return;
        }

        var modLoader = ml;

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (var mod : mods) {
            CompletableFuture<Void> future = checkMod(data, mod, modLoader, mcVersion);
            if (future != null) {
                futures.add(future);
            }
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenRunAsync(() -> {
                    Settings.webSocketAPI.sendMessage(new InstanceModsData.UpdateCheckingFinished(data));
                }, CreeperLauncher.taskExeggutor);
    }

    private static CompletableFuture<Void> checkMod(InstanceModsData data, ModInfo mod, ModpackVersionManifest.Target modLoader, String mcVersion) {
        var curseData = mod.curse();
        if (curseData == null) return null;

        return Constants.MOD_VERSION_CACHE.queryMod(curseData.curseProject()).thenAcceptAsync(m -> {
            if (m == null) return;

            var version = m.findLatestCompatibleVersion(modLoader.getName(), mcVersion);
            if (version == null) return;

            if (version.getId() <= curseData.curseFile()) return;
            Settings.webSocketAPI.sendMessage(new InstanceModsData.UpdateAvailable(
                    data,
                    mod,
                    new CurseMetadata(
                            m.getId(),
                            version.getId(),
                            version.getName(),
                            curseData.synopsis(),
                            curseData.icon()
                    )
            ));
        }, CreeperLauncher.taskExeggutor);
    }
}
