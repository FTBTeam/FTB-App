package net.creeperhost.creeperlauncher.api.handlers.instances;

import com.google.gson.annotations.JsonAdapter;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.quack.gson.PathTypeAdapter;
import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.api.WebSocketHandler;
import net.creeperhost.creeperlauncher.api.data.instances.InstalledInstancesData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.data.InstanceJson;
import net.creeperhost.creeperlauncher.pack.Instance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class InstalledInstancesHandler implements IMessageHandler<InstalledInstancesData> {
    @Override
    public void handle(InstalledInstancesData data) {
        if (data.refresh) {
            Instances.refreshInstances();
        }

        List<SugaredInstanceJson> instanceJsons = FastStream.of(Instances.allInstances())
                .map(SugaredInstanceJson::new)
                .toList();

        Set<String> availableCategories = instanceJsons.stream().map(e -> e.category).collect(Collectors.toSet());
        
        InstalledInstancesData.Reply reply = new InstalledInstancesData.Reply(data.requestId, instanceJsons, List.of(), availableCategories);
        WebSocketHandler.sendMessage(reply);
    }

    public static class SugaredInstanceJson extends InstanceJson {
        @JsonAdapter (PathTypeAdapter.class)
        public final Path path;
        public final boolean pendingCloudInstance;
        public final List<String> rootDirs = new ArrayList<>();

        public SugaredInstanceJson(InstanceJson other, Path path, boolean pendingCloudInstance) {
            super(other);
            this.path = path;
            this.pendingCloudInstance = pendingCloudInstance;

            try (var files = Files.list(this.path)) {
                this.rootDirs.addAll(
                    files
                        .filter(Files::isDirectory)
                        .map(e -> this.path.relativize(e).toString())
                        .toList()
                );
            } catch (IOException ignored) {}
        }
        
        public SugaredInstanceJson(Instance instance) {
            this(instance.props, instance.path, instance.isPendingCloudInstance());
        }
    }
}
