package net.creeperhost.creeperlauncher.api.handlers.importer;

import com.google.gson.annotations.JsonAdapter;
import net.covers1624.quack.gson.PathTypeAdapter;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.instance.importer.Importer;
import net.creeperhost.creeperlauncher.instance.importer.meta.SimpleInstanceInfo;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class ImportProviderInstancesHandler implements IMessageHandler<ImportProviderInstancesHandler.Data> {
    @Override
    public void handle(Data data) {
        try {
            var provider = Importer.allInstances(data.provider, data.location);
            Settings.webSocketAPI.sendMessage(new Reply(data, provider));
        } catch (IOException e) {
            // TODO: Real error
            throw new RuntimeException(e);
        }

        Settings.webSocketAPI.sendMessage(new Reply(data, List.of()));
    }
    
    public static class Data extends BaseData {
        @JsonAdapter(Importer.ProviderAdapter.class)
        public Importer.Providers provider;
        
        @Nullable
        @JsonAdapter(PathTypeAdapter.class)
        public Path location;
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
