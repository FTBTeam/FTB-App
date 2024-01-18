package net.creeperhost.creeperlauncher.api;

import net.creeperhost.creeperlauncher.api.data.BaseData;

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
//        DYNAMIC_ON_CONNECT,
        STATIC,
    }
}
