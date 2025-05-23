package dev.ftb.app.api.handlers.instances;

import com.google.gson.annotations.JsonAdapter;
import dev.ftb.app.Instances;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.instances.InstalledInstancesData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.data.InstanceJson;
import dev.ftb.app.pack.Instance;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.quack.gson.PathTypeAdapter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
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

        Set<String> availableCategories = new HashSet<>();
        availableCategories.add("Default");
        availableCategories.addAll(instanceJsons.stream().map(e -> e.category).toList());
        
        InstalledInstancesData.Reply reply = new InstalledInstancesData.Reply(data.requestId, instanceJsons, availableCategories);
        WebSocketHandler.sendMessage(reply);
    }

    public static class SugaredInstanceJson extends InstanceJson {
        @JsonAdapter (PathTypeAdapter.class)
        public final Path path;
        public final List<String> rootDirs = new ArrayList<>();

        public SugaredInstanceJson(InstanceJson other, Path path) {
            super(other);
            this.path = path;

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
            this(instance.props, instance.path);
        }
    }
}
