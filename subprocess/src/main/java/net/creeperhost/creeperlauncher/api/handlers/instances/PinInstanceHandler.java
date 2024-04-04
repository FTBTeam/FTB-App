package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.api.WebSocketHandler;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;

import java.io.IOException;
import java.util.UUID;

public class PinInstanceHandler implements IMessageHandler<PinInstanceHandler.Data> {
    @Override
    public void handle(Data data) {
        var instanceUuid = UUID.fromString(data.instance);
        var instance = Instances.getInstance(instanceUuid);
        
        if (instance == null) {
            WebSocketHandler.sendMessage(new Reply(data, false, null));
            return;
        }
        
        instance.props.pinned = data.pin;
        try {
            instance.saveJson();
            WebSocketHandler.sendMessage(new Reply(data, true, new InstalledInstancesHandler.SugaredInstanceJson(instance)));
        } catch (IOException e) {
            WebSocketHandler.sendMessage(new Reply(data, false, null));
        }
    }

    public static class Data extends BaseData {
        public String instance;
        public boolean pin;
    }
    
    public static class Reply extends BaseData {
        public boolean success;
        public InstalledInstancesHandler.SugaredInstanceJson instance;
        
        public Reply(Data data, boolean success, InstalledInstancesHandler.SugaredInstanceJson instance) {
            this.requestId = data.requestId;
            this.type = "pinInstanceReply";
            this.success = success;
            this.instance = instance;
        }
    }
}
