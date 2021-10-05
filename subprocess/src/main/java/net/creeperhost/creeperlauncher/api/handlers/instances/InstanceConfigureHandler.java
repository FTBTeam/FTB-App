package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.api.data.instances.InstanceConfigureData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.pack.LocalInstance;

import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

public class InstanceConfigureHandler implements IMessageHandler<InstanceConfigureData>
{
    @Override
    public void handle(InstanceConfigureData data)
    {
        try
        {
            //TODO, instance lookup?
            LocalInstance instance = new LocalInstance(Settings.getInstanceLocOr(Constants.INSTANCES_FOLDER_LOC).resolve(data.uuid));
            for (Map.Entry<String, String> setting : data.instanceInfo.entrySet())
            {
                switch (setting.getKey().toLowerCase())
                {
                    case "memory":
                        instance.memory = Integer.parseInt(setting.getValue());
                        break;
                    case "name":
                        instance.name = setting.getValue();
                        break;
                    case "jvmargs":
                        instance.jvmArgs = setting.getValue();
                        break;
                    case "width":
                        instance.width = Integer.parseInt(setting.getValue());
                        break;
                    case "height":
                        instance.height = Integer.parseInt(setting.getValue());
                        break;
                    case "cloudsaves":
                        instance.cloudSaves = Boolean.parseBoolean(setting.getValue());
                        break;
                    case "jrepath":
                        if(setting.getValue().length() == 0){
                            instance.embeddedJre = true;
                            instance.jrePath = null;
                        } else {
                            instance.embeddedJre = false;
                            instance.jrePath = Paths.get(setting.getValue());
                        }
                        break;
                }
            }
            instance.saveJson();
            Instances.refreshInstances();
            Settings.webSocketAPI.sendMessage(new InstanceConfigureData.Reply(data, "success"));
        } catch (Exception err)
        {
            Settings.webSocketAPI.sendMessage(new InstanceConfigureData.Reply(data, "error"));
        }

    }
}
