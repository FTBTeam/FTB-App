package net.creeperhost.creeperlauncher.api.handlers.importer;

import com.google.gson.annotations.JsonAdapter;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.instance.importer.Importer;
import net.creeperhost.creeperlauncher.util.Result;

import java.util.List;

public class ImportProviderImportHandler implements IMessageHandler<ImportProviderImportHandler.Data> {
    @Override
    public void handle(Data data) {
        var importer = Importer.factory(data.provider);
        for (var uuid : data.instanceUuids) {
            Result<Boolean, String> instance = importer.importInstance(uuid);
            Settings.webSocketAPI.sendMessage(new Reply(data, true));
        }
    }

    public static class Data extends BaseData {
        public @JsonAdapter(Importer.ProviderAdapter.class) Importer.Providers provider;
        public List<String> instanceUuids;
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
