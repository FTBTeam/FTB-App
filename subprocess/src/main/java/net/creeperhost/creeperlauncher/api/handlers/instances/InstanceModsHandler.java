package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.instances.InstanceModsData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.data.InstanceModifications;
import net.creeperhost.creeperlauncher.data.mod.CurseMetadata;
import net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class InstanceModsHandler implements IMessageHandler<InstanceModsData> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(InstanceModsData data) {
        var instance = Instances.getInstance(data.uuid);
        if (instance == null) return;

        var mods = instance.getMods();
        Settings.webSocketAPI.sendMessage(new InstanceModsData.Reply(data, mods));

        var mcVersion = instance.versionManifest.getTargetVersion("game");

        InstanceModifications modifications = instance.getModifications();
        ModpackVersionManifest.Target ml = modifications != null ? modifications.getModLoaderOverride() : null;
        if (ml == null) {
            ml = instance.versionManifest.findTarget("modloader");
        }

        if (ml == null || mcVersion == null) return;

        var modLoader = ml;

        for (var mod : mods) {
            var curseData = mod.curse();
            if (curseData == null) continue;

            Constants.MOD_VERSION_CACHE.queryMod(curseData.curseProject()).thenAcceptAsync(m -> {
                if (m == null) return;

                var version = m.findLatestCompatibleVersion(modLoader.getName(), mcVersion);
                if (version == null) return;

                if (version.getId() > curseData.curseFile()) return;
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
}
