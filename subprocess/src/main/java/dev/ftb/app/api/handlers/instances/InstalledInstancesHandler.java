package dev.ftb.app.api.handlers.instances;

import com.google.gson.annotations.JsonAdapter;
import dev.ftb.app.Instances;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.instances.InstalledInstancesData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.data.InstanceJson;
import dev.ftb.app.instance.InstanceCategory;
import dev.ftb.app.pack.Instance;
import net.covers1624.quack.gson.PathTypeAdapter;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class InstalledInstancesHandler implements IMessageHandler<InstalledInstancesData> {
    @Override
    public void handle(InstalledInstancesData data) {
        if (data.refresh) {
            Instances.refreshInstances();
        }

        List<SugaredInstanceJson> instanceJsons = Instances.allInstances()
                .stream()
                .map(SugaredInstanceJson::new)
                .toList();
        
        var categories = Instances.categories();
        if (categories.isEmpty()) {
            categories = new LinkedList<>();
            categories.add(InstanceCategory.DEFAULT);
        }
        
        InstalledInstancesData.Reply reply = new InstalledInstancesData.Reply(data.requestId, instanceJsons, categories);
        WebSocketHandler.sendMessage(reply);
    }

    public static class SugaredInstanceJson extends InstanceJson {
        @JsonAdapter (PathTypeAdapter.class)
        public final Path path;
        
        public final List<String> rootDirs = new ArrayList<>();
        
        @Nullable
        public final String artworkFile;
        
        public SugaredInstanceJson(Instance instance) {
            super(instance.props);
            
            this.path = instance.path;
            this.artworkFile = instance.logoArtwork.getBase64Image();

            try (var files = Files.list(this.path)) {
                this.rootDirs.addAll(
                    files
                        .filter(Files::isDirectory)
                        .map(e -> this.path.relativize(e).toString())
                        .toList()
                );
            } catch (IOException ignored) {}
        }
    }
}
