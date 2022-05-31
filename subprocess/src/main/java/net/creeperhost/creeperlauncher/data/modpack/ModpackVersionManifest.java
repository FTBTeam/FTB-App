package net.creeperhost.creeperlauncher.data.modpack;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import net.covers1624.quack.collection.StreamableIterable;
import net.covers1624.quack.gson.HashCodeAdapter;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.net.DownloadAction;
import net.covers1624.quack.net.okhttp.OkHttpDownloadAction;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.install.FileValidation;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Models the <code>modpacks.ch</code> api.
 * Reference: https://modpacksch.docs.apiary.io/#/reference/0/modpacks/version-manifest/200?mc=reference%2F0%2Fmodpacks%2Fversion-manifest%2F200
 * <p>
 * Created by covers1624 on 16/11/21.
 */
@SuppressWarnings ("FieldMayBeFinal") // Non-Final for Gson.
public class ModpackVersionManifest {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final Gson GSON = new Gson();

    public static final int MINIMUM_SPEC = 4096;
    public static final int RECOMMENDED_SPEC = 6132;

    @Nullable
    private String status;

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

    public ModpackVersionManifest() {
    }

    public ModpackVersionManifest(ModpackVersionManifest other) {
        status = other.status;
        id = other.id;
        parent = other.parent;
        name = other.name;
        specs = other.specs != null ? other.specs.copy() : null;
        type = other.type;
        targets = StreamableIterable.of(other.targets)
                .map(Target::copy)
                .toLinkedList();
        files = StreamableIterable.of(other.files)
                .map(ModpackFile::copy)
                .toLinkedList();
        installs = other.installs;
        plays = other.plays;
        updated = other.updated;
        refreshed = other.refreshed;
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
        return queryManifest(Constants.getCreeperhostModpackPrefix(isPrivate, packType) + packId + "/" + versionId);
    }

    @Nullable
    public static ModpackVersionManifest queryManifest(String url) throws IOException, JsonParseException {
        LOGGER.info("Querying Modpack version manifest: {}", url);
        StringWriter sw = new StringWriter();
        DownloadAction action = new OkHttpDownloadAction()
                .setClient(Constants.OK_HTTP_CLIENT)
                .setUserAgent(Constants.USER_AGENT)
                .setUrl(url)
                .setDest(sw);
        action.execute();
        
        ModpackVersionManifest manifest = JsonUtils.parse(GSON, sw.toString(), ModpackVersionManifest.class);
        if (manifest.getStatus().equals("error")) {
            // TODO Log error.
            return null;
        }

        return manifest;
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
        LinkedList<Target> targetsMatching = StreamableIterable.of(getTargets())
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

    public ModpackVersionManifest copy() {
        return new ModpackVersionManifest(this);
    }

    // @formatter:off
    public String getStatus() { return requireNonNull(status); }
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

        @Nullable // TODO, not sure what this data type is.
        private JsonArray tags;

        public long updated;

        public ModpackFile() {
        }

        public ModpackFile(long id, String path, String name, String url, HashCode sha1, long size) {
            this.id = id;
            this.path = path;
            this.name = name;
            this.url = url;
            this.sha1 = sha1;
            this.size = size;
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
        }

        public Path toPath(Path root) {
            return root.resolve(getPath()).resolve(getName());
        }

        public String instanceRelPath() {
            return StringUtils.appendIfMissing(StringUtils.removeStart(getPath(), "./"), "/") + StringUtils.removeStart(getName(), "/");
        }

        public Path getCfExtractPath(Path root, String modpackVersion) {
            return root.resolve(getPath()).resolve(modpackVersion + "-" + getName());
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
        public String getPath() { return requireNonNull(path); }
        public String getName() { return requireNonNull(name); }
        public String getUrl() { return requireNonNull(url); }
        public HashCode getSha1() { return requireNonNull(sha1); }
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
