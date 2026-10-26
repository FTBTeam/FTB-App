package dev.ftb.app.api;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.ftb.app.Constants;
import dev.ftb.app.api.WebsocketServer.PortMode;
import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;
import dev.ftb.app.services.Services;
import dev.ftb.app.util.MiscUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class RcpApi {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new Gson();

    private @Nullable WebsocketServer server;
    private final RcpEndpoints endpoints;
    
    private final Services services;
    
    public RcpApi(Services services) {
        this.endpoints = new RcpEndpoints();
        this.services = services;
    }
    
    public int start(PortMode portMode) {
        int port = switch (portMode) {
            case DYNAMIC -> MiscUtils.getRandomEphemeralPort();
            case STATIC -> Constants.WEBSOCKET_PORT;
        };
        server = new JettyWebsocketServer(port, this::handleMessage);
        server.startServer();
        
        return port;
    }

    public void stop() {
        if (server == null) return;
        server.stopServer();
    }

    public void sendMessage(BaseData data) {
        if (server == null) return;
        server.sendMessage(data);
    }

    public int getPort() {
        assert server != null : "Websocket not configured yet.";
        return server.getPort();
    }

    public void handleMessage(String data) {
        JsonElement parse = JsonParser.parseString(data);
        if (!parse.isJsonObject()) return;

        JsonObject jsonObject = parse.getAsJsonObject();
        if (!jsonObject.has("type")) return;

        String type = jsonObject.get("type").getAsString();
        Pair<Class<? extends BaseData>, IMessageHandler<? extends BaseData>> entry = this.endpoints.fromName(type);
        if (entry == null) {
            LOGGER.error("No handler for message type '{}'", type);
            return;
        }
        IMessageHandler<? extends BaseData> iMessageHandler = entry.getRight();
        if (iMessageHandler == null) {
            LOGGER.error("No handler for message type '{}'", type);
            return;
        }

        try {
            BaseData parsedData = GSON.fromJson(data, entry.getLeft());
            if (this.services.flags().isDev() || (parsedData.secret != null && parsedData.secret.equals(Constants.WEBSOCKET_SECRET))) {
                var context = new MessageContext(this.services);
                
                CompletableFuture.runAsync(() -> iMessageHandler.handle(context, parsedData), this.services.threads().messageHandlerPool()).exceptionally((t) -> {
                    LOGGER.error("Error handling message", t);
                    return null;
                });
            }
        } catch (Exception e) {
            LOGGER.error("Failed to parse message.", e);
        }
    }
}
