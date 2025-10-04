package dev.ftb.app.api.handlers.instances;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.ftb.app.Instances;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.instances.InstanceConfigureData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.instance.InstanceCategory;
import dev.ftb.app.pack.Instance;
import dev.ftb.app.util.MiscUtils;
import net.covers1624.jdkutils.JavaInstall;
import net.covers1624.quack.gson.JsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;
import java.util.function.Function;

public class InstanceConfigureHandler implements IMessageHandler<InstanceConfigureData> {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(InstanceConfigureData data) {
        try {
            Instance instance = Instances.getInstance(data.uuid);
            if (instance == null) {
                WebSocketHandler.sendMessage(new InstanceConfigureData.Reply(data, "error", "Unable to save settings as the instance couldn't be found..."));
                return;
            }

            JsonObject updateJson = JsonUtils.parseRaw(data.instanceJson).getAsJsonObject();

            instance.props.name = getOrDefault(updateJson, "name", JsonElement::getAsString, instance.props.name);
            instance.props.jvmArgs = getOrDefault(updateJson, "jvmArgs", JsonElement::getAsString, instance.props.jvmArgs);
            instance.props.shellArgs = getOrDefault(updateJson, "shellArgs", JsonElement::getAsString, instance.props.shellArgs);
            instance.props.programArgs = getOrDefault(updateJson, "programArgs", JsonElement::getAsString, instance.props.programArgs);

            Path jreRealPath = instance.props.jrePath;
            if (updateJson.has("jrePath")) {
                String jrePath = updateJson.get("jrePath").getAsString();
                if (!jrePath.isEmpty()) {
                    jreRealPath = Paths.get(jrePath);
                } else {
                    jreRealPath = null;
                }
            }

            instance.props.jrePath = jreRealPath;
            instance.props.memory = getOrDefault(updateJson, "memory", JsonElement::getAsInt, instance.props.memory);
            instance.props.width = getOrDefault(updateJson, "width", JsonElement::getAsInt, instance.props.width);
            instance.props.height = getOrDefault(updateJson, "height", JsonElement::getAsInt, instance.props.height);
            instance.props.fullscreen = getOrDefault(updateJson, "fullScreen", JsonElement::getAsBoolean, instance.props.fullscreen);
            instance.props.releaseChannel = getOrDefault(updateJson, "releaseChannel", JsonElement::getAsString, instance.props.releaseChannel);
            instance.props.categoryId = getOrDefault(updateJson, "categoryId", (elm) -> {
                if (elm.isJsonNull()) {
                    return null;
                }

                return MiscUtils.tryParseUuid(elm.getAsString());
            }, InstanceCategory.DEFAULT.uuid());
            instance.props.embeddedJre = jreRealPath == null;
            
            // Support for unlocking a modpack
            instance.props.locked = getOrDefault(updateJson, "locked", JsonElement::getAsBoolean, instance.props.locked);
            instance.props.preventMetaModInjection = getOrDefault(updateJson, "preventMetaModInjection", JsonElement::getAsBoolean, instance.props.preventMetaModInjection);
            
            var instanceImage = getOrDefault(updateJson, "instanceImage", JsonElement::getAsString, null);
            if (instanceImage != null && instanceImage.startsWith("data:")) {
                try {
                    String[] parts = instanceImage.split(",");
                    // Eww
                    String type = parts[0].split(";")[0].split(":")[1].split("/")[1];
                    String base64Data = parts[1];

                    // Create a input stream from the base64 data
                    byte[] decodedBytes = Base64.getDecoder().decode(base64Data);
                    ByteArrayInputStream inputStream = new ByteArrayInputStream(decodedBytes);
                    instance.logoArtwork.saveImage(inputStream, type);
                    inputStream.close();
                } catch (IOException e) {
                    LOGGER.error("Failed to save instance image", e);
                }
            }

            if (jreRealPath != null) {
                if (JavaInstall.parse(jreRealPath) == null) {
                    WebSocketHandler.sendMessage(new InstanceConfigureData.Reply(data, "error", "No java install found... Make sure you're selecting the 'java' file in '/bin'."));
                    return;
                }
                instance.props.jrePath = jreRealPath;
            }

            instance.saveJson();
            WebSocketHandler.sendMessage(new InstanceConfigureData.Reply(data, "success", "", new InstalledInstancesHandler.SugaredInstanceJson(instance)));
        } catch (IOException ex) {
            LOGGER.error("Failed to update instance settings.", ex);
            WebSocketHandler.sendMessage(new InstanceConfigureData.Reply(data, "error", "Fatal error on settings saving..."));
        }
    }

    private <T> T getOrDefault(JsonObject object, String key, Function<JsonElement, T> reader, T defaultValue) {
        if (object.has(key) && !object.get(key).isJsonNull()) {
            return reader.apply(object.get(key));
        } else {
            return defaultValue;
        }
    }
}
