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
        ModPack pack = FTBModPackInstallerTask.getPackFromAPI(instance.getId(), instance.getVersionId(), data._private, instance.packType);
        if (pack != null) {
            List<ModFile> instanceMods = instance.getMods();
            List<ModFile> packMods = pack.getMods();
            List<ModFile> finalMergedMods = new ArrayList<>();
            packMods.forEach(mod -> {
                int indexOf = instanceMods.indexOf(mod);
                if (indexOf != -1) {
                    ModFile instanceMod = instanceMods.get(indexOf);
                    finalMergedMods.add(new ModFile(mod.getName(), mod.getVersion(), instanceMod.getSize(), mod.getSha1()).setExists(true).setExpected(true));
                } else {
                    finalMergedMods.add(mod);
                }
            });

            instanceMods.forEach(mod -> {
                if (!finalMergedMods.contains(mod)) {
                    finalMergedMods.add(mod);
                }
            });
            List<ModFile> collect = finalMergedMods.stream().sorted((e1, e2) -> e1.getName().compareToIgnoreCase(e2.getName())).collect(Collectors.toList());
            Settings.webSocketAPI.sendMessage(new InstanceModsData.Reply(data, collect));
        }
    }
}
