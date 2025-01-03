package dev.ftb.app.api.handlers.instances;

import com.google.gson.JsonObject;
import dev.ftb.app.AppMain;
import dev.ftb.app.api.data.instances.MessageClientData;
import dev.ftb.app.api.handlers.IMessageHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MessageClientHandler implements IMessageHandler<MessageClientData> {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(MessageClientData data) {
        try {
            if (AppMain.socket != null && AppMain.socket.isConnected())
            {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("instance", data.uuid);
                jsonObject.addProperty("message", data.message);
                AppMain.socketWrite.write((jsonObject.toString()+"\n").getBytes());
            }
        } catch (Throwable e) {
            LOGGER.warn("Error sending message to Minecraft client", e);
        }
    }
}
