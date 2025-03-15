package dev.ftb.app.api.handlers.other;

import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.BaseData;
import dev.ftb.app.api.handlers.IMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppInitHandler implements IMessageHandler<AppInitHandler.Data> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppInitHandler.class);
    
    @Override
    public void handle(Data data) {
        boolean success = true;
        Reply.Builder reply = Reply.builder(data);
        
        reply.setSuccess(success);
        WebSocketHandler.sendMessage(reply.build());
    }
    
    public static class Data extends BaseData {}
    
    public static class Reply extends Data {
        boolean success = false;
        String errorMessage = "";

        private Reply(Data data) {
            super();
            this.requestId = data.requestId;
            this.type = "appInitReply";
        }
        
        public static Builder builder(Data data) {
            return new Builder(data);
        }
        
        public static class Builder {
            private final Reply reply;
            
            private Builder(Data data) {
                reply = new Reply(data);
            }
            
            
            public Builder setSuccess(boolean success) {
                reply.success = success;
                return this;
            }
            
            public Builder setErrorMessage(String errorMessage) {
                reply.errorMessage = errorMessage;
                return this;
            }
            
            public Reply build() {
                return reply;
            }
        }
    }
}
