package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.instances.InstanceModToggleData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.api.handlers.ModFile;
import net.creeperhost.creeperlauncher.pack.LocalInstance;

import java.util.Optional;
import java.util.UUID;

public class InstanceModToggleHandler implements IMessageHandler<InstanceModToggleData> {

    @Override
    public void handle(InstanceModToggleData data) {
        LocalInstance instance = Instances.getInstance(UUID.fromString(data.uuid));

        Optional<ModFile> mod = instance.getMods().stream()
                .filter(e -> e.getName().equals(data.fileName))
                .findFirst();

        boolean successful = mod
                .map(e -> e.setEnabled(data.state))
                .orElse(false);

        Settings.webSocketAPI.sendMessage(new InstanceModToggleData.Reply(data, successful, mod.map(ModFile::isEnabled).orElse(true)));
    }
}
