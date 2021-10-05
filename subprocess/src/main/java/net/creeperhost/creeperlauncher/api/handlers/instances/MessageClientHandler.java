package net.creeperhost.creeperlauncher.api.handlers.instances;

import com.google.gson.JsonObject;
import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.api.data.instances.MessageClientData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MessageClientHandler implements IMessageHandler<MessageClientData> {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(MessageClientData data) {
        try {
            if (CreeperLauncher.socket != null && CreeperLauncher.socket.isConnected())
            {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("instance", data.uuid);
                jsonObject.addProperty("message", data.message);
                CreeperLauncher.socketWrite.write((jsonObject.toString()+"\n").getBytes());
            }
        } catch (Throwable e) {
            LOGGER.warn("Error sending message to Minecraft client", e);
        }
    }
}
