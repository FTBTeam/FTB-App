package net.creeperhost.creeperlauncher.data.modpack;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import net.covers1624.quack.gson.HashCodeAdapter;
import net.creeperhost.creeperlauncher.install.FileValidation;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Models the <code>modpacks.ch</code> api.
 * Reference: https://modpacksch.docs.apiary.io/#/reference/0/modpacks/version-manifest/200?mc=reference%2F0%2Fmodpacks%2Fversion-manifest%2F200
 * <p>
 * Created by covers1624 on 16/11/21.
 */
@SuppressWarnings ("FieldMayBeFinal") // Non-Final for Gson.
public class ModpackVersionManifest {

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
    private Specs specs;
    @Nullable
    private String type;
    private List<Target> targets = new LinkedList<>();
    private List<ModpackFile> files = new LinkedList<>();

    private long installs;
    private long plays;

    private long updated;
    private long refreshed;

    public int getMinimumSpec() {
        return specs != null ? specs.minimum : MINIMUM_SPEC;
    }

    public int getRecommendedSpec() {
        return specs != null ? specs.recommended : RECOMMENDED_SPEC;
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

        public Path toPath(Path root) {
            return root.resolve(getPath()).resolve(getName());
        }

        public FileValidation getValidation() {
            return FileValidation.of()
                    .withHash(Hashing.sha1(), getSha1())
                    .withExpectedSize(getSize());
        }

        // @formatter:off
        public long getId() { return id; }
        public String getVersion() { return requireNonNull(version); }
        public String getPath() { return requireNonNull(path); }
        public String getName() { return requireNonNull(name); }
        public String getUrl() { return requireNonNull(url); }
        public HashCode getSha1() { return requireNonNull(sha1); }
        public long getSize() { return size; }
        public boolean isClientOnly() { return clientOnly; }
        public boolean isServerOnly() { return serverOnly; }
        public boolean isOptional() { return optional; }
        public String getType() { return requireNonNull(type); }
        public long getUpdated() { return updated; }
        // @formatter:on
    }

}
