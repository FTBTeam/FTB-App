package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.covers1624.jdkutils.JavaLocator;
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
                Settings.webSocketAPI.sendMessage(new InstanceConfigureData.Reply(data, "error", "Unable to save settings as the instance couldn't be found..."));
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
                            var jreLocation = Paths.get(setting.getValue());
                            if (JavaLocator.parseInstall(jreLocation) == null) {
                                Settings.webSocketAPI.sendMessage(new InstanceConfigureData.Reply(data, "error", "No java install found... Make sure you're selecting the 'java' file in '/bin'."));
                                return;
                            }
                            instance.jrePath = jreLocation;
                        }
                        break;
                    case "shellargs":
                        instance.shellArgs = setting.getValue();
                        break;
                }
            }
            instance.saveJson();
            Settings.webSocketAPI.sendMessage(new InstanceConfigureData.Reply(data, "success", ""));
        } catch (Exception err)
        {
            Settings.webSocketAPI.sendMessage(new InstanceConfigureData.Reply(data, "error", "Fatal error on settings saving..."));
        }

    }
}
