package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.instances.InstanceModsData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.api.handlers.ModFile;
import net.creeperhost.creeperlauncher.install.tasks.FTBModPackInstallerTask;
import net.creeperhost.creeperlauncher.pack.ModPack;
import net.creeperhost.creeperlauncher.pack.LocalInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class InstanceModsHandler implements IMessageHandler<InstanceModsData> {

    @Override
    public void handle(InstanceModsData data) {
        LocalInstance instance = Instances.getInstance(UUID.fromString(data.uuid));
        if (instance == null) return;

        List<ModFile> packMods = instance.versionManifest.toLegacyFiles();
        List<ModFile> cleanMods = instance.getMods(false).stream()
                .map((currentMod) ->
                        packMods.stream()
                                .filter(e -> e.getName().equals(currentMod.getName()))
                                .findFirst()
                                .map(e -> new ModFile(currentMod.getRealName(), currentMod.getVersion(), e.getSize(), currentMod.getSha1()).setExists(true).setExpected(true))
                                .orElse(currentMod)
                )
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .collect(Collectors.toList());

        Settings.webSocketAPI.sendMessage(new InstanceModsData.Reply(data, cleanMods));
    }
}
