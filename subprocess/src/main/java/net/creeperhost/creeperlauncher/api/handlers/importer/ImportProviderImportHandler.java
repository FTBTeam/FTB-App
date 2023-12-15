package net.creeperhost.creeperlauncher.api.handlers.importer;

import com.google.gson.annotations.JsonAdapter;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.instance.importer.Importer;
import net.creeperhost.creeperlauncher.util.Result;

import java.nio.file.Path;
import java.util.List;

public class ImportProviderImportHandler implements IMessageHandler<ImportProviderImportHandler.Data> {
    @Override
    public void handle(Data data) {
        var importer = Importer.factory(data.provider);
        for (var instanceLocation : data.locations) {
            var instancePath = Path.of(instanceLocation);
            Result<Boolean, String> instance = importer.importInstance(instancePath);
            // TODO: handle result
            Settings.webSocketAPI.sendMessage(new Reply(data, true));
        }
    }

    public static class Data extends BaseData {
        public @JsonAdapter(Importer.ProviderAdapter.class) Importer.Providers provider;
        
        public List<String> locations;
    }

    public static class Reply extends ImportProviderImportHandler.Data {
        public boolean success;

        public Reply(ImportProviderImportHandler.Data data, boolean success) {
            this.type = data.type + "Reply";
            this.requestId = data.requestId;
            this.provider = data.provider;
            this.success = success;
        }
    }
}
