package dev.ftb.app.api;

import com.google.gson.Gson;
import dev.ftb.app.AppMain;
import dev.ftb.app.api.data.BaseData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeHandler;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class JettyWebsocketServer implements WebsocketServer {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final boolean DEBUG = Boolean.getBoolean("WebSocket.debug");
    private static final Gson GSON = new Gson();
    
    private static final long MAX_MESSAGE_SIZE = 64L * 1024 * 1024; // Jetty defaults to a fairly small payload size but our app can send large messages. This is the same limit as the frontend.

    private final int port;
    private final Server server;
    private final List<Connection> connections = Collections.synchronizedList(new ArrayList<>());
    private final List<String> buffer = Collections.synchronizedList(new ArrayList<>());
    
    private final Consumer<String> onMessage;
    
    public JettyWebsocketServer(int port, Consumer<String> onMessage) {
        this.onMessage = onMessage;
        this.port = port;

        QueuedThreadPool threadPool = new QueuedThreadPool(16, 2);
        threadPool.setName("WebSocket");
        threadPool.setDaemon(true); // Don't hold the JVM open on shutdown.

        server = new Server(threadPool);

        ServerConnector connector = new ServerConnector(server);
        connector.setHost("127.0.0.1");
        connector.setPort(port);
        server.addConnector(connector);

        WebSocketUpgradeHandler wsHandler = WebSocketUpgradeHandler.from(server, container -> {
            container.setMaxTextMessageSize(MAX_MESSAGE_SIZE);
            container.setMaxBinaryMessageSize(MAX_MESSAGE_SIZE);
            container.setMaxFrameSize(MAX_MESSAGE_SIZE);
            container.setIdleTimeout(Duration.ZERO);
            
            container.addMapping("/", (req, resp, cb) -> new Connection());
        });
        wsHandler.setHandler(new Handler.Abstract() {
            @Override
            public boolean handle(Request request, Response response, org.eclipse.jetty.util.Callback callback) {
                Response.writeError(request, response, callback, 400, "Websocket required");
                return true;
            }
        });
        server.setHandler(wsHandler);
    }

    @Override
    public void startServer() {
        LOGGER.info("Starting websocket server on 127.0.0.1:{}", port);
        try {
            server.start();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to start websocket.", ex);
        }
        LOGGER.info("Websocket started!");
    }

    @Override
    public void stopServer() {
        LOGGER.info("Gracefully stopping websocket.");
        try {
            server.stop();
        } catch (Exception ex) {
            LOGGER.warn("Failed to stop websocket cleanly.", ex);
        }
        LOGGER.info("Websocket stopped.");
    }

    @Override
    public void sendMessage(BaseData data) {
        String msg = GSON.toJson(data);
        if (connections.isEmpty()) {
            synchronized (buffer) {
                if (connections.isEmpty()) { // Not really sure if this is needed but it seems like a good idea to double check.
                    buffer.add(msg);
                    return;
                }
            }
        }
        synchronized (connections) {
            connections.forEach(conn -> conn.send(msg));
        }
    }

    @Override
    public int getPort() {
        return port;
    }

    private void addConnection(Connection conn) {
        synchronized (buffer) {
            buffer.forEach(conn::send);
            buffer.clear();
            connections.add(conn);
        }
    }

    /**
     * Jetty websocket connection handler. Each connection gets its own instance of this class.
     * TODO: We likely want to avoid multiple connections from the client in the future as the internal
     *       API's do not handle this well. 
     */
    public class Connection extends Session.Listener.AbstractAutoDemanding {
        @Override
        public void onWebSocketOpen(Session session) {
            super.onWebSocketOpen(session);
            LOGGER.info("New connection to {}.", session.getRemoteSocketAddress());
            addConnection(this);
            AppMain.websocketDisconnect = false;
        }

        public void send(String msg) {
            if (DEBUG) {
                LOGGER.info("WS :> {}", msg);
            }
            if (!isOpen()) return;

            synchronized (this) {
                getSession().sendText(msg, Callback.from(() -> { }, ex -> LOGGER.warn("Failed to send message.", ex)));
            }
        }

        @Override
        public void onWebSocketText(String msg) {
            if (DEBUG) {
                LOGGER.info("WS <: {}", msg);
            }
            onMessage.accept(msg);
        }

        @Override
        public void onWebSocketClose(int statusCode, String reason, Callback callback) {
            LOGGER.info("Connection to {} closed {}({}).", getSession().getRemoteSocketAddress(), reason, statusCode);
            connections.remove(this);
            AppMain.websocketDisconnect = connections.isEmpty();
            callback.succeed();
        }

        @Override
        public void onWebSocketError(Throwable cause) {
            LOGGER.warn("Error on websocket.", cause);
        }
    }
}
