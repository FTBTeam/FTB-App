package net.creeperhost.creeperlauncher.pack.json;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Partially models the <code>modpacks.ch</code> api.
 * <p>
 * The data modeled here is only the data required for app operation
 * and does not reflect the entire spec.
 * <p>
 * Created by covers1624 on 16/11/21.
 */
public class VersionManifest {

    public static final int MINIMUM_SPEC = 4096;
    public static final int RECOMMENDED_SPEC = 6132;

    @Nullable
    public String status;

    @Nullable
    public String name;
    @Nullable
    public Specs specs;
    public List<Target> targets = new ArrayList<>();
    public List<File> files = new ArrayList<>();

    public int getMinimumSpec() {
        return specs != null ? specs.minimum : MINIMUM_SPEC;
    }

    public int getRecommendedSpec() {
        return specs != null ? specs.recommended : RECOMMENDED_SPEC;
    }

    public static class Specs {

        public int minimum;
        public int recommended;
    }

    public static class Target {

        @Nullable
        public String version;
        @Nullable
        public String name;
        @Nullable
        public String type;
    }

    public static class File {

        @Nullable
        public String version;
        @Nullable
        public String path;
        @Nullable
        public String url;
        @Nullable
        public String sha1;
        public int size;
        @SerializedName ("clientonly")
        public boolean clientOnly;
        @SerializedName ("serveronly")
        public boolean serverOnly;
        public boolean optional;
        public int id;
        @Nullable
        public String name;
        @Nullable
        public String type;

    }

}
