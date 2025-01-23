package dev.ftb.app.api;

import com.google.gson.Gson;
import dev.ftb.app.AppMain;
import dev.ftb.app.api.data.BaseData;
import fi.iki.elonen.NanoWSD;
import net.covers1624.quack.reflect.PrivateLookups;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NanoHttpdWebsocketServer extends NanoWSD implements WebsocketServer {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final boolean DEBUG = Boolean.getBoolean("WebSocket.debug");
    private static final Gson GSON = new Gson();

    private final int port;
    private final PortMode portMode;
    private final List<Connection> connections = Collections.synchronizedList(new ArrayList<>());
    private final List<String> buffer = Collections.synchronizedList(new ArrayList<>());

    public NanoHttpdWebsocketServer(int port, PortMode portMode) {
        super("127.0.0.1", port);
        this.port = port;
        this.portMode = portMode;
    }

    @Override
    protected WebSocket openWebSocket(IHTTPSession handshake) {
        return new Connection(handshake);
    }

    @Override
    protected Response serveHttp(IHTTPSession session) {
        return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Websocket required");
    }

    @Override
    public void startServer() {
        LOGGER.info("Starting websocket server on {}:{}", getHostname(), port);
        try {
            start(-1, true);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to start websocket.", ex);
        }
        LOGGER.info("Websocket started!");
    }

    @Override
    public void stopServer() {
        LOGGER.info("Gracefully stopping websocket.");
        stop();
        LOGGER.info("Websocket stopped.");
    }

    @Override
    public void sendMessage(BaseData data) {
        String msg = GSON.toJson(data);
        if (connections.isEmpty()) {
            synchronized (buffer) {
                if (connections.isEmpty()) { // Double checked style, yay.
                    buffer.add(msg);
                    return;
                }
            }
        }
        connections.forEach(conn -> conn.send(msg));
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

    private class Connection extends WebSocket {

        private static final VarHandle state;
        private static final MethodHandle doClose;

        static {
            try {
                MethodHandles.Lookup lookup = PrivateLookups.getTrustedLookup();
                state = lookup.unreflectVarHandle(WebSocket.class.getDeclaredField("state"));
                doClose = lookup.unreflect(WebSocket.class.getDeclaredMethod("doClose", WebSocketFrame.CloseCode.class, String.class, boolean.class));
            } catch (Throwable ex) {
                LOGGER.error("Failed to do reflection!", ex);
                throw new RuntimeException("Failed to do reflection!", ex);
            }
        }

        public Connection(IHTTPSession handshakeRequest) {
            super(handshakeRequest);
        }

        @Override
        protected void onOpen() {
            LOGGER.info("New connection to {}.", getHandshakeRequest().getRemoteIpAddress());
//            if (portMode == PortMode.DYNAMIC_ON_CONNECT) {
//                int port = MiscUtils.getRandomEphemeralPort();
//                LOGGER.info("Changing websocket port.");
//                send("{\"port\": \"" + port + "\", \"secret\": \"" + Constants.WEBSOCKET_SECRET + "\"}");
//                try {
//                    close(WebSocketFrame.CloseCode.NormalClosure, "change_port", false);
//                } catch (IOException ex) {
//                    LOGGER.warn("Failed to close close.", ex);
//                }
//                WebSocketHandler.restartOnPort(port);
//                return;
//            }

            addConnection(this);
            AppMain.websocketDisconnect = false;
        }

        @Override
        public void send(String msg) {
            if (DEBUG) {
                LOGGER.info("WS :> {}", msg);
            }
            try {
                super.send(msg);
            } catch (IOException ex) {
                throw new RuntimeException("Failed to send message.", ex);
            }
        }

        @Override
        public void close(WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) throws IOException {
            // TODO fixes bug in parent impl..
            State oldState = (State) state.get(this);
            state.set(this, State.CLOSING);
            if (oldState == State.OPEN && !initiatedByRemote) {
                sendFrame(new WebSocketFrame.CloseFrame(code, reason));
            } else {
                try {
                    doClose.invoke(this, code, reason, initiatedByRemote);
                } catch (Throwable ex) {
                    throw new RuntimeException("Unable to do reflection invoke?", ex);
                }
            }
        }

        @Override
        protected void onClose(WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
            LOGGER.info("Connection to {} closed {}({}).", getHandshakeRequest().getRemoteIpAddress(), reason, code);
            connections.remove(this);
            AppMain.websocketDisconnect = connections.isEmpty();
        }

        @Override
        protected void onMessage(WebSocketFrame message) {
            String msg = message.getTextPayload();
            if (DEBUG) {
                LOGGER.info("WS <: {}", msg);
            }
            WebSocketHandler.handleMessage(msg);
        }

        @Override
        protected void onPong(WebSocketFrame pong) {
            LOGGER.info("Pong received.");
        }

        @Override
        protected void onException(IOException exception) {
            LOGGER.warn("Error on websocket.", exception);
        }
    }
}
