package dev.ftb.app.api;

import dev.ftb.app.api.data.BaseData;

/**
 * Created by covers1624 on 11/12/23.
 */
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
