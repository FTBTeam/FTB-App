package net.creeperhost.creeperlauncher.api.handlers.other;

import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;

public class MineTogetherAuthenticationHandler implements IMessageHandler<MineTogetherAuthenticationHandler.Data> {
    @Override
    public void handle(Data data) {
        System.out.printf("MineTogetherAuthenticationHandler: %s\n", data);
    }

    public static class Data extends BaseData {
        String authType;
        String apiKey;
        String appToken;
    }
    
    public static class Reply extends Data {
        boolean success;
        
        public Reply(Data data) {
            this.requestId = data.requestId;
            this.type = "mineTogetherAuthenticationReply";
            
            this.success = false;
        }
    }
}
