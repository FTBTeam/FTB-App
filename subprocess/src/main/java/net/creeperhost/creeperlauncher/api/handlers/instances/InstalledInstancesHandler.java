package net.creeperhost.creeperlauncher.api.handlers.instances;

import com.google.gson.annotations.JsonAdapter;
import net.covers1624.quack.collection.StreamableIterable;
import net.covers1624.quack.gson.PathTypeAdapter;
import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.instances.InstalledInstancesData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.data.InstanceJson;

import java.nio.file.Path;
import java.util.List;

public class InstalledInstancesHandler implements IMessageHandler<InstalledInstancesData> {

    @Override
    public void handle(InstalledInstancesData data) {
        if (data.refresh) {
            Instances.refreshInstances();
        }

        List<InstanceJson> instanceJsons = StreamableIterable.of(Instances.allInstances())
                .<InstanceJson>map(e -> new SugaredInstanceJson(e.props, e.path))
                .toLinkedList();
        InstalledInstancesData.Reply reply = new InstalledInstancesData.Reply(data.requestId, instanceJsons, List.of());
        Settings.webSocketAPI.sendMessage(reply);
    }

    public static class SugaredInstanceJson extends InstanceJson {

        @JsonAdapter (PathTypeAdapter.class)
        public final Path path;

        public SugaredInstanceJson(InstanceJson other, Path path) {
            super(other);
            this.path = path;
        }
    }
}
