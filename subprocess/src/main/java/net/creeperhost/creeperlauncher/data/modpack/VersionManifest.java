package net.creeperhost.creeperlauncher.data.modpack;

import com.google.gson.JsonArray;
import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

/**
 * Models the <code>modpacks.ch</code> api.
 * Reference: https://modpacksch.docs.apiary.io/#/reference/0/modpacks/version-manifest/200?mc=reference%2F0%2Fmodpacks%2Fversion-manifest%2F200
 * <p>
 * Created by covers1624 on 16/11/21.
 */
public class VersionManifest {

    public static final int MINIMUM_SPEC = 4096;
    public static final int RECOMMENDED_SPEC = 6132;

    @Nullable
    public String status;

    public long id;
    public long parent;

    @Nullable
    public String name;
    @Nullable
    public Specs specs;
    @Nullable
    public String type;
    public List<Target> targets = new LinkedList<>();
    public List<File> files = new LinkedList<>();

    public long installs;
    public long plays;

    public long updated;
    public long refreshed;

    public int getMinimumSpec() {
        return specs != null ? specs.minimum : MINIMUM_SPEC;
    }

    public int getRecommendedSpec() {
        return specs != null ? specs.recommended : RECOMMENDED_SPEC;
    }

    public static class Specs {

        public long id;

        public int minimum;
        public int recommended;
    }

    public static class Target {

        public long id;

        @Nullable
        public String version;
        @Nullable
        public String name;
        @Nullable
        public String type;

        public long updated;
    }

    public static class File {

        public long id;

        @Nullable
        public String version;
        @Nullable
        public String path;
        @Nullable
        public String name;
        @Nullable
        public String url;
        @Nullable
        public String sha1;
        public long size;
        @SerializedName ("clientonly")
        public boolean clientOnly;
        @SerializedName ("serveronly")
        public boolean serverOnly;
        public boolean optional;
        @Nullable
        public String type;

        @Nullable // TODO, not sure what this data type is.
        private JsonArray tags;

        public long updated;

    }

}
