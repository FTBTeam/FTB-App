package net.creeperhost.creeperlauncher.api.handlers.instances;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.covers1624.jdkutils.JavaLocator;
import net.covers1624.quack.gson.JsonUtils;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.api.data.instances.InstanceConfigureData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.pack.Instance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class InstanceConfigureHandler implements IMessageHandler<InstanceConfigureData> {
    @Override
    public void handle(InstanceConfigureData data) {
        try
        {
            Instance instance = Instances.getInstance(data.uuid);
            if (instance == null) {
                // TODO, This message needs to be improved. We should tell the frontend _why_
                Settings.webSocketAPI.sendMessage(new InstanceConfigureData.Reply(data, "error", "Unable to save settings as the instance couldn't be found..."));
                return;
            }

            JsonObject updateJson = JsonUtils.parseRaw(data.instanceJson).getAsJsonObject();
            
            instance.props.name = getOrDefault(updateJson, "name", JsonElement::getAsString, instance.props.name);
            instance.props.jvmArgs = getOrDefault(updateJson, "jvmArgs", JsonElement::getAsString, instance.props.jvmArgs);
            
            Path jreRealPath = instance.props.jrePath;
            if (updateJson.has("jrePath")) {
                String jrePath = updateJson.get("jrePath").getAsString();
                jreRealPath = Paths.get(jrePath);
            }
            
            instance.props.jrePath = jreRealPath;
            instance.props.memory = getOrDefault(updateJson, "memory", JsonElement::getAsInt, instance.props.memory);
            instance.props.width = getOrDefault(updateJson, "width", JsonElement::getAsInt, instance.props.width);
            instance.props.height = getOrDefault(updateJson, "height", JsonElement::getAsInt, instance.props.height);
            instance.props.cloudSaves = getOrDefault(updateJson, "cloudSaves", JsonElement::getAsBoolean, instance.props.cloudSaves);
            instance.props.embeddedJre = jreRealPath == null;
            
            if (jreRealPath != null) {
                if (JavaLocator.parseInstall(jreRealPath) == null) {
                    Settings.webSocketAPI.sendMessage(new InstanceConfigureData.Reply(data, "error", "No java install found... Make sure you're selecting the 'java' file in '/bin'."));
                    return;
                }
                instance.props.jrePath = jreRealPath;
            }
            
            instance.saveJson();
            Settings.webSocketAPI.sendMessage(new InstanceConfigureData.Reply(data, "success", "", new InstalledInstancesHandler.SugaredInstanceJson(instance)));
        } catch (Exception err) {
            err.printStackTrace();
            Settings.webSocketAPI.sendMessage(new InstanceConfigureData.Reply(data, "error", "Fatal error on settings saving..."));
        }
    }

    private <T> T getOrDefault(JsonObject object, String key, Function<JsonElement, T> reader, T defaultValue) {
        if (object.has(key)) {
            return reader.apply(object.get(key));
        } else {
            return defaultValue;
        }
    }
}
