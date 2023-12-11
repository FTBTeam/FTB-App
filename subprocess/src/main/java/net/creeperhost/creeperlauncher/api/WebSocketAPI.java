package net.creeperhost.creeperlauncher.api;

import java.io.IOException;
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
    public static final Logger LOGGER = LogManager.getLogger();

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
        WebSocketHandler.handleMessage(message);
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message)
    {
        // Log in debug to see if anything is ever getting piped to this method
        LOGGER.debug("received ByteBuffer from {}", conn.getRemoteSocketAddress());
        
        // Decoded data
        String decoded = new String(message.array());
        LOGGER.debug("Decoded: {}", decoded);
    }

    @Override
    public void onError(WebSocket conn, Exception ex)
    {
        try {
            CreeperLauncher.websocketDisconnect = true;
            LOGGER.error("an error occurred on connection {}", conn == null ? "[connection not set]" : conn.getRemoteSocketAddress(), ex);
        } catch (Exception ignored) {
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

    @Override
    public void stop() throws IOException, InterruptedException {
        LOGGER.info("Shutting down websockets on {}", this.getAddress());
        super.stop();
    }

    // TODO: ensure thread safety
    public void sendMessage(BaseData data)
    {
        String s = GsonUtils.GSON.toJson(data);
        if (getConnections().isEmpty() || !fullyConnected)
        {
            notConnectedQueue.add(s);
        } else {
            for (WebSocket client : getConnections()) {
                // I have no idea why the connection would still be there if it was marked as not open, but apparently it can be!
                // This library is getting nuked at some point to resolve this bs..
                if (!client.isOpen()) continue;
                client.send(s);
            }
        }
    }
}
