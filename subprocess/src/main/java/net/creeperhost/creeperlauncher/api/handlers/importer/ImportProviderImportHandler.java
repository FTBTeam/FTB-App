package net.creeperhost.creeperlauncher.api.handlers.importer;

import com.google.gson.annotations.JsonAdapter;
import net.covers1624.quack.gson.LowerCaseEnumAdapterFactory;
import net.creeperhost.creeperlauncher.api.WebSocketHandler;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.api.handlers.instances.InstalledInstancesHandler;
import net.creeperhost.creeperlauncher.instance.importer.Providers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.List;

public class ImportProviderImportHandler implements IMessageHandler<ImportProviderImportHandler.Data> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(Data data) {
        for (var instanceLocation : data.locations) {
            var instancePath = Path.of(instanceLocation);
            try {
                InstalledInstancesHandler.SugaredInstanceJson instance = data.provider.provider.importInstance(instancePath);
                WebSocketHandler.sendMessage(new Reply(data, true));
            } catch (Throwable ex) {
                LOGGER.error("Failed to import instance.", ex);
                WebSocketHandler.sendMessage(new Reply(data, true));
            }
        }
    }

    public static class Data extends BaseData {

        @JsonAdapter (LowerCaseEnumAdapterFactory.class)
        public Providers provider;

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
