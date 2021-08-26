package net.creeperhost.creeperlauncher.api;

import java.net.BindException;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.util.GsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class WebSocketAPI extends WebSocketServer
{
    private static final Logger LOGGER = LogManager.getLogger();

    private boolean fullyConnected = false;

    public WebSocketAPI(InetSocketAddress address)
    {
        super(address);
    }

    int connections = 0;

    private static final ConcurrentLinkedQueue<String> notConnectedQueue = new ConcurrentLinkedQueue<>();

    public static Random random = new Random();

    public static int generateRandomPort() {
        return random.nextInt(9999) + 10000;
    }

    public static String generateSecret() {
        return UUID.randomUUID().toString();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake)
    {
        if (CreeperLauncher.defaultWebsocketPort && !CreeperLauncher.isDevMode)
        {
            conn.send("{\"port\": \"" + CreeperLauncher.websocketPort + "\", \"secret\": \"" + CreeperLauncher.websocketSecret + "\"}");
            conn.close();
            LOGGER.info("Front end connected: {} - sending our socket and secret and relaunching websocket", conn.getRemoteSocketAddress());
            CreeperLauncher.defaultWebsocketPort = false;
            Settings.webSocketAPI = new WebSocketAPI(new InetSocketAddress(InetAddress.getLoopbackAddress(), CreeperLauncher.websocketPort));
            Settings.webSocketAPI.start();
            try {
                stop();
            } catch (Exception ignored) {}
            return;
        } else {
            CreeperLauncher.websocketDisconnect = false;
        }

        LOGGER.info("Front end connected: {}", conn.getRemoteSocketAddress());
        fullyConnected = true;
        connections++;
        notConnectedQueue.forEach(conn::send);
        notConnectedQueue.clear();
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote)
    {
        connections--;
        if (connections == 0) CreeperLauncher.websocketDisconnect = true;
        LOGGER.info("closed {} with exit code {} additional info: {}", conn.getRemoteSocketAddress(), code, reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message)
    {
        WebSocketMessengerHandler.handleMessage(message);
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message)
    {
    }

    @Override
    public void onError(WebSocket conn, Exception ex)
    {
        try
        {
            CreeperLauncher.websocketDisconnect = true;
            LOGGER.error("an error occurred on connection {}", conn.getRemoteSocketAddress(), ex);
        } catch (NullPointerException ignored)
        {
            if(ex instanceof BindException) {
                CreeperLauncher.websocketPort = generateRandomPort();
                LOGGER.info("New Port: {}", CreeperLauncher.websocketPort);
                Settings.webSocketAPI = new WebSocketAPI(new InetSocketAddress(InetAddress.getLoopbackAddress(), CreeperLauncher.websocketPort));
                Settings.webSocketAPI.start();
                try {
                    stop();
                } catch (Exception ignored1) {}
            }
        }
    }

    @Override
    public void onStart()
    {
        LOGGER.info("Server started successfully - {}", Constants.APPVERSION);
    }

    // TODO: ensure thread safety
    public void sendMessage(BaseData data)
    {
        String s = GsonUtils.GSON.toJson(data);
        if (getConnections().isEmpty() || !fullyConnected)
        {
            notConnectedQueue.add(s);
        } else {
            getConnections().forEach((client) -> client.send(s));
        }
    }
}
