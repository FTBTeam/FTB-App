package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.api.data.instances.InstanceSetModEnabledData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.pack.LocalInstance;

import java.util.UUID;
import java.util.stream.Collectors;

public class InstanceSetModEnabledHandler implements IMessageHandler<InstanceSetModEnabledData> {
    @Override
    public void handle(InstanceSetModEnabledData data) {
        LocalInstance instance = Instances.getInstance(UUID.fromString(data.uuid));
        boolean result = instance.getMods().stream().filter(mod -> mod.getName().equals(data.name)).limit(1).map(mod -> mod.setEnabled(data.state)).collect(Collectors.toList()).get(0);
    }
}