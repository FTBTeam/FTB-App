package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.covers1624.jdkutils.JavaLocator;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.api.data.instances.InstanceConfigureData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.pack.Instance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Paths;
import java.util.Map;

public class InstanceConfigureHandler implements IMessageHandler<InstanceConfigureData>
{

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(InstanceConfigureData data)
    {
        try
        {
            Instance instance = Instances.getInstance(data.uuid);
            if (instance == null) {
                // TODO, This message needs to be improved. We should tell the frontend _why_
                Settings.webSocketAPI.sendMessage(new InstanceConfigureData.Reply(data, "error", "Unable to save settings as the instance couldn't be found..."));
                return;
            }
            for (Map.Entry<String, String> setting : data.instanceInfo.entrySet())
            {
                LOGGER.debug("Frontend set {} to {} for instance {}.", setting.getKey(), setting.getValue(), instance.getUuid());
                switch (setting.getKey().toLowerCase())
                {
                    case "memory":
                        instance.props.memory = Integer.parseInt(setting.getValue());
                        break;
                    case "name":
                        instance.props.name = setting.getValue();
                        break;
                    case "jvmargs":
                        instance.props.jvmArgs = setting.getValue();
                        break;
                    case "width":
                        instance.props.width = Integer.parseInt(setting.getValue());
                        break;
                    case "height":
                        instance.props.height = Integer.parseInt(setting.getValue());
                        break;
                    case "cloudsaves":
                        instance.props.cloudSaves = Boolean.parseBoolean(setting.getValue());
                        break;
                    case "jrepath":
                        if(setting.getValue().length() == 0){
                            instance.props.embeddedJre = true;
                            instance.props.jrePath = null;
                        } else {
                            instance.props.embeddedJre = false;
                            var jreLocation = Paths.get(setting.getValue());
                            if (JavaLocator.parseInstall(jreLocation) == null) {
                                Settings.webSocketAPI.sendMessage(new InstanceConfigureData.Reply(data, "error", "No java install found... Make sure you're selecting the 'java' file in '/bin'."));
                                return;
                            }
                            instance.props.jrePath = jreLocation;
                        }
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
