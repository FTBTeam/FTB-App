package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.instances.InstanceModsData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.data.mod.ModInfo;
import net.creeperhost.creeperlauncher.data.mod.ModManifest;
import net.creeperhost.creeperlauncher.pack.Instance;
import net.creeperhost.creeperlauncher.util.ModVersionCache.CachedMod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class InstanceModsHandler implements IMessageHandler<InstanceModsData> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(InstanceModsData data) {
        Instance instance = Instances.getInstance(data.uuid);
        if (instance == null) return;

        List<ModInfo> cleanInfo = instance.getMods();

        List<CompletableFuture<CachedMod>> futures = new LinkedList<>();
        Settings.webSocketAPI.sendMessage(new InstanceModsData.Reply(data, sugarModInfos(cleanInfo, futures)));
        if (!futures.isEmpty()) {
            CreeperLauncher.taskExeggutor.execute(() -> {
                LOGGER.info("Awaiting {} futures for dynamic mods list update for instance {}.", futures.size(), data.uuid);
                int i = 0;
                for (CompletableFuture<CachedMod> future : futures) {
                    try {
                        i++;
                        future.get();
                        LOGGER.info(" Completed {}/{} Futures.", i, futures.size());
                    } catch (InterruptedException | ExecutionException e) {
                        LOGGER.error("Error waiting for future.", e);
                    }
                }

                Settings.webSocketAPI.sendMessage(new InstanceModsData.Reply(data, sugarModInfos(cleanInfo, null)));
                LOGGER.info("Updated!");
            });
        }
    }

    private static List<ModInfo> sugarModInfos(List<ModInfo> infos, @Nullable List<CompletableFuture<CachedMod>> futures) {
        for (ListIterator<ModInfo> iterator = infos.listIterator(); iterator.hasNext(); ) {
            ModInfo modInfo = iterator.next();
            long project = modInfo.curseProject();
            long version = modInfo.curseFile();
            // No point.
            if (project == -1 && version == -1) continue;

            // Get the future for the mod info.
            CompletableFuture<CachedMod> future = Constants.MOD_VERSION_CACHE.queryVersion(project, version);
            if (!future.isDone()) {
                // If we need to wait for the future, try and add it to the list.
                if (futures != null) futures.add(future);
                // We will come back to this later on a subsequent call to this method.
                continue;
            }
            CachedMod cachedMod;
            try {
                cachedMod = future.get();
            } catch (InterruptedException | ExecutionException ex) {
                LOGGER.error("Future failed to run.", ex);
                continue;
            }
            if (cachedMod == null) continue;

            iterator.set(new ModInfo(modInfo, cachedMod.version().getName(), cachedMod.description()));
        }

        return infos;
    }
}
