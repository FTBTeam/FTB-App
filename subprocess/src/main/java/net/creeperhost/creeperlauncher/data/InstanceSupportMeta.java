package net.creeperhost.creeperlauncher.data;

import com.google.common.hash.HashCode;
import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import net.covers1624.quack.collection.StreamableIterable;
import net.covers1624.quack.gson.HashCodeAdapter;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.platform.OperatingSystem;
import net.covers1624.quack.util.MultiHasher.HashFunc;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.install.tasks.NewDownloadTask;
import net.creeperhost.creeperlauncher.install.tasks.NewDownloadTask.DownloadValidation;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import static java.util.Objects.requireNonNull;

/**
 * Created by covers1624 on 11/2/22.
 */
@SuppressWarnings ("FieldMayBeFinal") // Gson
public class InstanceSupportMeta {

    private static final Gson GSON = new Gson();
    private static final String URL = "https://apps.modpacks.ch/meta.json";

    private List<SupportEntry> supportMods = new LinkedList<>();
    private List<SupportEntry> supportAgents = new LinkedList<>();

    public static InstanceSupportMeta update() throws IOException {
        Path metaFile = Constants.BIN_LOCATION.resolve("support-meta.json");
        NewDownloadTask task = NewDownloadTask.builder()
                .url(URL)
                .dest(metaFile)
                .withValidation(DownloadValidation.of().withUseETag(true).withUseOnlyIfModified(true))
                .build();
        task.execute(null, null);

        return JsonUtils.parse(GSON, metaFile, InstanceSupportMeta.class);
    }

    public List<SupportFile> getSupportMods(String type) {
        return StreamableIterable.of(supportMods)
                .filter(e -> e.getType().equals(type))
                .flatMap(SupportEntry::getFiles)
                .toList();
    }

    public List<SupportEntry> getSupportAgents() {
        return supportAgents;
    }

    public static class SupportEntry {

        @Nullable
        private String type;

        private List<SupportFile> files = new LinkedList<>();

        // @formatter:off
        public String getType() { return requireNonNull(type); }
        public List<SupportFile> getFiles() { return files; }
        // @formatter:on
    }

    public static class SupportFile {

        @Nullable
        private String name;
        private List<String> mcVersions = new LinkedList<>();
        private List<String> platforms = new LinkedList<>();
        @Nullable
        private String url;
        private List<ChecksumEntry> checksums = new LinkedList<>();

        // @formatter:off
        public String getName() { return requireNonNull(name); }
        public List<String> getMcVersions() { return mcVersions; }
        public List<String> getPlatforms() { return platforms; }
        public String getUrl() { return requireNonNull(url); }
        public List<ChecksumEntry> getChecksums() { return checksums; }
        // @formatter:on

        public boolean canApply(String modLoader, OperatingSystem current) {
            if (!mcVersions.isEmpty() && mcVersions.stream().noneMatch(modLoader::startsWith)) {
                return false;
            }
            if (!platforms.isEmpty() && platforms.stream().noneMatch(e -> current.equals(OperatingSystem.parse(e)))) {
                return false;
            }
            return true;
        }

        public DownloadValidation makeValidation() {
            DownloadValidation validation = DownloadValidation.of();
            for (ChecksumEntry checksum : getChecksums()) {
                validation = validation.withHash(checksum.getType(), checksum.getHash());
            }
            return validation;
        }

        public NewDownloadTask createTask(Path destFolder) {
            return NewDownloadTask.builder()
                    .url(getUrl())
                    .dest(destFolder.resolve(getName()))
                    .withValidation(makeValidation())
                    .build();
        }
    }

    public static class ChecksumEntry {

        @Nullable
        @JsonAdapter (HashFuncAdapter.class)
        private HashFunc type;
        @Nullable
        @JsonAdapter (HashCodeAdapter.class)
        private HashCode hash;

        // @formatter:off
        public HashFunc getType() { return requireNonNull(type); }
        public HashCode getHash() { return requireNonNull(hash); }
        // @formatter:on
    }

    private static class HashFuncAdapter implements JsonDeserializer<HashFunc> {

        @Override
        @Nullable
        public HashFunc deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonNull()) {
                return null;
            }
            return HashFunc.find(json.getAsString().toUpperCase(Locale.ROOT));
        }
    }
}

