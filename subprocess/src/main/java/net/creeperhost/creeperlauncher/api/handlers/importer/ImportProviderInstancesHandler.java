package net.creeperhost.creeperlauncher.api.handlers.importer;

import com.google.gson.annotations.JsonAdapter;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.instance.importer.Importer;
import net.creeperhost.creeperlauncher.instance.importer.meta.SimpleInstanceInfo;

import java.util.List;

public class ImportProviderInstancesHandler implements IMessageHandler<ImportProviderInstancesHandler.Data> {
    @Override
    public void handle(Data data) {
        var instances = Importer.factory(data.provider).getAllInstances();
        Settings.webSocketAPI.sendMessage(new Reply(data, instances));
    }
    
    public static class Data extends BaseData {
        public @JsonAdapter(Importer.ProviderAdapter.class) Importer.Providers provider;
    }
    
    public static class Reply extends Data {
        public List<SimpleInstanceInfo> instances;
        
        public Reply(Data data, List<SimpleInstanceInfo> instances) {
            this.type = data.type + "Reply";
            this.requestId = data.requestId;
            this.provider = data.provider;
            this.instances = instances;
        }
    }
}
