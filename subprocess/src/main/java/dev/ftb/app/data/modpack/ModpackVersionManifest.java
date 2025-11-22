package dev.ftb.app.data.modpack;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import dev.ftb.app.Constants;
import dev.ftb.app.install.FileValidation;
import dev.ftb.app.util.FileUtils;
import dev.ftb.app.util.ModpackApiUtils;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.quack.gson.HashCodeAdapter;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.net.DownloadAction;
import net.covers1624.quack.net.okhttp.OkHttpDownloadAction;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@SuppressWarnings ("FieldMayBeFinal") // Non-Final for Gson.
public class ModpackVersionManifest {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final Gson GSON = new Gson();

    public static final int INVALID_ID = -1000;

    public static final int MINIMUM_SPEC = 4096;
    public static final int RECOMMENDED_SPEC = 6132;

    @Nullable
    private String status;
    @Nullable
    private String message;

    private long id;
    private long parent;

    @Nullable
    private String name;
    @Nullable
    @JsonAdapter (SpecsSerializer.class)
    private Specs specs;
    @Nullable
    private String type;
    private List<Target> targets = new LinkedList<>();
    private List<ModpackFile> files = new LinkedList<>();

    private long installs;
    private long plays;

    private long updated;
    private long refreshed;

    @Nullable
    public transient Path cfExtractOverride;

    public ModpackVersionManifest() {
    }

    public ModpackVersionManifest(ModpackVersionManifest other) {
        status = other.status;
        id = other.id;
        parent = other.parent;
        name = other.name;
        specs = other.specs != null ? other.specs.copy() : null;
        type = other.type;
        targets = FastStream.of(other.targets)
                .map(Target::copy)
                .toLinkedList();
        files = FastStream.of(other.files)
                .map(ModpackFile::copy)
                .toLinkedList();
        installs = other.installs;
        plays = other.plays;
        updated = other.updated;
        refreshed = other.refreshed;
    }

    public static ModpackVersionManifest makeInvalid() {
        ModpackVersionManifest manifest = new ModpackVersionManifest();
        manifest.id = INVALID_ID;
        manifest.parent = INVALID_ID;
        manifest.name = "Corrupt instance install.";
        return manifest;
    }

    @Nullable
    public static Pair<ModpackManifest, ModpackVersionManifest> queryManifests(long packId, long versionId, boolean isPrivate, byte packType) throws IOException, JsonParseException {
        ModpackManifest modpackManifest = ModpackManifest.queryManifest(packId, isPrivate, packType);
        if (modpackManifest != null) {
            ModpackVersionManifest versionManifest = queryManifest(packId, versionId, isPrivate, packType);
            if (versionManifest != null) {
                return Pair.of(
                        modpackManifest,
                        versionManifest
                );
            }
        }
        modpackManifest = ModpackManifest.queryManifest(packId, !isPrivate, packType);

        if (modpackManifest == null) return null; // We tried, really doesn't exist..

        return Pair.of(
                modpackManifest,
                queryManifest(packId, versionId, !isPrivate, packType)
        );
    }

    @Nullable
    public static ModpackVersionManifest queryManifest(long packId, long versionId, boolean isPrivate, byte packType) throws IOException, JsonParseException {        
        return queryManifest(ModpackApiUtils.getModpacksEndpoint(isPrivate, packType) + packId + "/" + versionId);
    }

    @Nullable
    public static ModpackVersionManifest queryManifest(String url) throws IOException, JsonParseException {
        LOGGER.info("Querying Modpack version manifest: {}", url);
        StringWriter sw = new StringWriter();
        DownloadAction action = new OkHttpDownloadAction()
                .setClient(Constants.httpClient())
                .setUserAgent(Constants.USER_AGENT)
                .setUrl(url)
                .setDest(sw);
        
        ModpackApiUtils.injectBearerHeader(action);
        
        action.execute();

        ModpackVersionManifest manifest = JsonUtils.parse(GSON, sw.toString(), ModpackVersionManifest.class);
        if (manifest.getStatus().equals("error")) {
            LOGGER.error("Failed to request manifest got: " + manifest.getMessage());
            return null;
        }

        return manifest;
    }

    @Nullable
    public static ModpackVersionManifest convert(String inputData) throws IOException {
        Request.Builder builder = new Request.Builder()
                .url(ModpackApiUtils.getModpacksApi() + "/curseforge/import")
                .put(RequestBody.create(inputData, MediaType.parse("application/json; charset=utf-8")));
        
        ModpackApiUtils.injectBearerHeader(builder);
        
        try (Response response = Constants.httpClient().newCall(builder.build()).execute()) {
            ResponseBody body = response.body();
            if (body == null) {
                LOGGER.error("Request returned empty body. Status code: {}", response.code());
                return null;
            }
            ModpackVersionManifest versionManifest = JsonUtils.parse(GSON, body.string(), ModpackVersionManifest.class);
            if (versionManifest.getStatus().equals("error")) {
                LOGGER.error("Failed to convert pack. Got error: {}", versionManifest.getMessage());
                return null;
            }
            return versionManifest;
        }
    }

    public int getMinimumSpec() {
        return specs != null ? specs.minimum : MINIMUM_SPEC;
    }

    public int getRecommendedSpec() {
        return specs != null ? specs.recommended : RECOMMENDED_SPEC;
    }

    /**
     * Finds a {@link Target} of a given type in the manifest.
     *
     * @param type The 'type' to find.
     * @return The {@link Target}.
     * @throws IllegalStateException If more than one target of the given type is found.
     */
    @Nullable
    public Target findTarget(String type) throws IllegalStateException {
        LinkedList<Target> targetsMatching = FastStream.of(getTargets())
                .filter(e -> type.equals(e.getType()))
                .toLinkedList();

        if (targetsMatching.size() > 1) {
            // Should be impossible??
            String desc = targetsMatching.stream().map(e -> e.getName() + "@" + e.getVersion()).collect(Collectors.joining(", ", "[", "]"));
            throw new IllegalStateException("Found more than one target for type '" + type + "'. " + desc);
        }

        return !targetsMatching.isEmpty() ? targetsMatching.getFirst() : null;
    }

    /**
     * Finds the version of a {@link Target} of a given type in the manifest.
     *
     * @param type The 'type' to find.
     * @return The {@link Target}'s version if found, otherwise {@code null}.
     * @throws IllegalStateException If more than one target of the given type is found.
     */
    @Nullable
    public String getTargetVersion(String type) throws IllegalStateException {
        Target target = findTarget(type);

        return target != null ? target.getVersion() : null;
    }

    /**
     * Count the number of targets which share this type.
     *
     * @param type The types.
     * @return The count.
     */
    public int getTargetCount(String type) {
        return FastStream.of(getTargets())
                .filter(e -> type.equals(e.getType()))
                .count();
    }

    public ModpackVersionManifest copy() {
        return new ModpackVersionManifest(this);
    }

    // @formatter:off
    public String getStatus() { return requireNonNull(status); }
    public String getMessage() { return requireNonNull(message); }
    public long getId() { return id; }
    public long getParent() { return parent; }
    public String getName() { return requireNonNull(name); }
    public Specs getSpecs() { return requireNonNull(specs); }
    public String getType() { return requireNonNull(type); }
    public List<Target> getTargets() { return targets; }
    public List<ModpackFile> getFiles() { return files; }
    public long getInstalls() { return installs; }
    public long getPlays() { return plays; }
    public long getUpdated() { return updated; }
    public long getRefreshed() { return refreshed; }
    // @formatter:on

    public static class Specs {

        private long id;

        private int minimum;
        private int recommended;

        public Specs() {
        }

        public Specs(Specs other) {
            id = other.id;
            minimum = other.minimum;
            recommended = other.recommended;
        }

        public Specs copy() {
            return new Specs(this);
        }
    }

    public static class Target {

        private long id;

        @Nullable
        private String version;
        @Nullable
        private String name;
        @Nullable
        private String type;

        private long updated;

        public Target() {
        }

        public Target(Target other) {
            id = other.id;
            version = other.version;
            name = other.name;
            type = other.type;
            updated = other.updated;
        }

        public Target copy() {
            return new Target(this);
        }

        // @formatter:off
        public long getId() { return id; }
        public String getVersion() { return requireNonNull(version); }
        public String getName() { return requireNonNull(name); }
        public String getType() { return requireNonNull(type); }
        public long getUpdated() { return updated; }
        // @formatter:on
    }

    public static class ModpackFile {

        private long id;

        @Nullable
        private String version;
        @Nullable
        private String path;
        @Nullable
        private String name;
        @Nullable
        private String url;
        private List<String> mirror = new LinkedList<>();
        @Nullable
        @JsonAdapter (HashCodeAdapter.class)
        private HashCode sha1;
        private long size;
        @SerializedName ("clientonly")
        private boolean clientOnly;
        @SerializedName ("serveronly")
        private boolean serverOnly;
        private boolean optional;
        @Nullable
        private String type;
        @Nullable
        private FileHashes hashes;

        @Nullable // TODO, not sure what this data type is.
        private JsonArray tags;

        public long updated;

        public ModpackFile() {
        }

        public ModpackFile(long id, String path, String name, String url, HashCode sha1, long size, String type) {
            this.id = id;
            this.path = path;
            this.name = name;
            this.url = url;
            this.sha1 = sha1;
            this.size = size;
            this.type = type;
        }

        public ModpackFile(ModpackFile other) {
            id = other.id;
            version = other.version;
            path = other.path;
            name = other.name;
            url = other.url;
            sha1 = other.sha1;
            size = other.size;
            clientOnly = other.clientOnly;
            serverOnly = other.serverOnly;
            optional = other.optional;
            type = other.type;
            tags = other.tags != null ? other.tags.deepCopy() : null;
            updated = other.updated;
            hashes = other.hashes;
        }

        public Path toPath(Path root) {
            return toPath(root, "");
        }

        public Path toPath(Path root, String nameSuffix) {
            String name = getName() + nameSuffix;
            String path = getPath();
            try {
                return root.resolve(path).resolve(name);
            } catch (InvalidPathException exception) {
                // Try again without formatting
                String plaintextName = name.replaceAll("§(?:\\w|\\d)", "");
                String plaintextPath = path.replaceAll("§(?:\\w|\\d)", "");
                return root.resolve(plaintextPath).resolve(plaintextName);
            }
        }

        public String instanceRelPath() {
            return StringUtils.appendIfMissing(StringUtils.removeStart(getPath(), "./"), "/") + StringUtils.removeStart(getName(), "/");
        }

        public Path getCfExtractPath(Path root, String modpackVersion) {
            return root.resolve(getPath()).resolve(FileUtils.stripInvalidChars(modpackVersion + "-" + getName()));
        }

        public FileValidation createValidation() {
            FileValidation validation = FileValidation.of();
            // Not sure if size can ever be -1, but sure.
            if (size != -1) {
                validation = validation.withExpectedSize(size);
            }
            // sha1 might be null
            if (sha1 != null) {
                validation = validation.withHash(Hashing.sha1(), sha1);
            }
            return validation;
        }

        public ModpackFile copy() {
            return new ModpackFile(this);
        }

        // @formatter:off
        public long getId() { return id; }
        public String getVersion() { return requireNonNull(version); }
        public @Nullable String getVersionOrNull() { return version; }
        public String getPath() { return requireNonNull(path); }
        public String getName() { return requireNonNull(name); }
        public String getUrl() { return requireNonNull(url); }
        public List<String> getMirror() { return mirror; }
        public HashCode getSha1() { return requireNonNull(sha1); }
        @Nullable public FileHashes getHashesOrNull() { return hashes; }
        @Nullable public HashCode getSha1OrNull() { return sha1; }
        public long getSize() { return size; }
        public boolean isClientOnly() { return clientOnly; }
        public boolean isServerOnly() { return serverOnly; }
        public boolean isOptional() { return optional; }
        public String getType() { return requireNonNull(type); }
        public long getUpdated() { return updated; }
        public void setUrl(@Nullable String url) { this.url = url; }
        public void setSha1(HashCode sha1) { this.sha1 = sha1; }
        public void setSize(long size) { this.size = size; }
        // @formatter:on
    }
    
    public static final class FileHashes {
        @JsonAdapter (HashCodeAdapter.class)
        public HashCode sha1;
        
        @JsonAdapter (HashCodeAdapter.class)
        public HashCode sha256;
        
        @JsonAdapter (HashCodeAdapter.class)
        public HashCode sha512;
        
        public long murmur;
        public long cfMurmur;
    }

    private static final class SpecsSerializer implements JsonDeserializer<Specs>, JsonSerializer<Specs> {

        @Nullable
        @Override
        public Specs deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (!json.isJsonObject()) return null;
            return context.deserialize(json, typeOfT);
        }

        @Override
        public JsonElement serialize(Specs src, Type typeOfSrc, JsonSerializationContext context) {
            return context.serialize(src, typeOfSrc);
        }
    }

}
