package dev.ftb.app.api;

import dev.ftb.app.api.data.BaseData;

public interface WebsocketServer {

    void startServer();

    void stopServer();

    void sendMessage(BaseData data);

    int getPort();

    enum PortMode {
        DYNAMIC,
        STATIC,
    }
}
