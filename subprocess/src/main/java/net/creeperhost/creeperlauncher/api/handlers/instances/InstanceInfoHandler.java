package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.instances.InstanceInfoData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.pack.ModPack;
import net.creeperhost.creeperlauncher.pack.LocalInstance;

import java.util.HashMap;

public class InstanceInfoHandler implements IMessageHandler<InstanceInfoData>
{
    @Override
    public void handle(InstanceInfoData data)
    {
        HashMap<String, String> instanceInfo = new HashMap<>();
        ModPack packInfo = null;
        try
        {
            LocalInstance instance = new LocalInstance(Settings.getInstanceLocOr(Constants.INSTANCES_FOLDER_LOC).resolve(data.uuid));
            instanceInfo.put("uuid", instance.getUuid().toString());
            instanceInfo.put("name", instance.getName());
            instanceInfo.put("memory", String.valueOf(instance.memory));
            instanceInfo.put("jvmargs", instance.jvmArgs);
            instanceInfo.put("width", String.valueOf(instance.width));
            instanceInfo.put("height", String.valueOf(instance.height));
            instanceInfo.put("embeddedjre", String.valueOf(instance.embeddedJre));
            packInfo = instance.getManifest(() -> {
                Settings.webSocketAPI.sendMessage(new InstanceInfoData.Reply(data, instanceInfo, instance.manifest));
            });

        } catch (Exception ignored)
        {
        }
        Settings.webSocketAPI.sendMessage(new InstanceInfoData.Reply(data, instanceInfo, packInfo));
    }
}
