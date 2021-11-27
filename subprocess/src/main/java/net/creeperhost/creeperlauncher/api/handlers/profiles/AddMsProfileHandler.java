package net.creeperhost.creeperlauncher.api.handlers.profiles;

import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;

public class AddMsProfileHandler implements IMessageHandler<AddMsProfileHandler.Data> {
    @Override
    public void handle(Data data) {
//        Settings.webSocketAPI.sendMessage(new Reply(data, profiles, activeProfileRaw));
    }

    public static class Data extends BaseData {

    }

    private static class Reply extends Data {
        public Reply(Data data) {
            this.requestId = data.requestId;
            this.type = data.type + "Reply";
        }
    }
}
