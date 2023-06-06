package net.creeperhost.creeperlauncher.api.handlers.other;

import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;

/**
 * Created by covers1624 on 17/2/23.
 */
public class OpenDebugToolsHandler implements IMessageHandler<BaseData> {

    @Override
    public void handle(BaseData data) {
        CreeperLauncher.openDebugTools();
    }
}
