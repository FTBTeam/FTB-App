package dev.ftb.app.api.handlers.instances;

import com.google.gson.annotations.JsonAdapter;
import dev.ftb.app.api.data.instances.InstalledInstancesData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;
import dev.ftb.app.data.InstanceJson;
import dev.ftb.app.instance.InstanceCategory;
import dev.ftb.app.pack.Instance;
import net.covers1624.quack.gson.PathTypeAdapter;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class InstalledInstancesHandler implements IMessageHandler<InstalledInstancesData> {
    @Override
    public void handle(MessageContext context, InstalledInstancesData data) {
        if (data.refresh) {
            context.services().instances().refreshInstances();
        }

        List<SugaredInstanceJson> instanceJsons = context.services().instances().allInstances()
                .stream()
                .map(SugaredInstanceJson::new)
                .toList();
        
        var categories = context.services().instances().categories();
        if (categories.isEmpty()) {
            categories = new LinkedList<>();
            categories.add(InstanceCategory.DEFAULT);
        }
        
        context.reply(new InstalledInstancesData.Reply(data.requestId, instanceJsons, categories));
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
