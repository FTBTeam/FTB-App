package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.api.data.instances.InstanceConfigureData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.pack.LocalInstance;

import java.nio.file.Paths;
import java.util.Map;

public class InstanceConfigureHandler implements IMessageHandler<InstanceConfigureData>
{
    @Override
    public void handle(InstanceConfigureData data)
    {
        try
        {
            LocalInstance instance = Instances.getInstance(data.uuid);
            if (instance == null) {
                // TODO, This message needs to be improved. We should tell the frontend _why_
                Settings.webSocketAPI.sendMessage(new InstanceConfigureData.Reply(data, "error"));
                return;
            }
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
            Settings.webSocketAPI.sendMessage(new InstanceConfigureData.Reply(data, "success"));
        } catch (Exception err)
        {
            Settings.webSocketAPI.sendMessage(new InstanceConfigureData.Reply(data, "error"));
        }

    }
}
