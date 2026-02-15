package dev.ftb.app.data;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import dev.ftb.app.data.modpack.ModpackVersionManifest;
import dev.ftb.app.pack.Instance;
import dev.ftb.app.util.HashingUtils;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.quack.gson.JsonUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class InstanceModifications {

    public static final Gson GSON = new Gson();

    private @Nullable ModpackVersionManifest.Target modLoaderOverride;

    private final List<ModOverride> overrides = new LinkedList<>();
    private boolean requiresMurmurFix = true;

    public static @Nullable InstanceModifications load(Path path, Instance instance) throws IOException, JsonParseException {
        if (!Files.exists(path)) return null;

        InstanceModifications modifications = JsonUtils.parse(GSON, path, InstanceModifications.class);
        if (modifications.requiresMurmurFix) {
            for (ModOverride override : modifications.overrides) {
                if (override.getMurmurHash() == -1) {
                    var modPath = instance.path.resolve("mods").resolve(override.getFileName());
                    if (Files.exists(modPath)) {
                        try {
                            var bytes = Files.readAllBytes(modPath);
                            override.murmurHash = HashingUtils.createCurseForgeMurmurHash(bytes);
                        } catch (Exception e) {
                            override.murmurHash = 0;
                        }
                    }
                }
            }
            
            modifications.requiresMurmurFix = false;
            save(path, modifications);
        }
        
        return modifications;
    }

    public static void save(Path path, InstanceModifications modifications) throws IOException {
        JsonUtils.write(GSON, path, modifications);
    }

    // @formatter:off
    public @Nullable ModpackVersionManifest.Target getModLoaderOverride() { return modLoaderOverride; }
    public List<ModOverride> getOverrides() { return overrides; }
    public void setModLoaderOverride(@Nullable ModpackVersionManifest.Target modLoaderOverride) { this.modLoaderOverride = modLoaderOverride; }
    // @formatter:on

    public @Nullable ModOverride findOverride(long id) {
        return FastStream.of(overrides)
                .filter(e -> e.getId() == id)
                .firstOrDefault();
    }

    public @Nullable ModOverride findOverride(String fileName) {
        return FastStream.of(overrides)
                .filter(e -> e.getFileName().equals(fileName))
                .firstOrDefault();
    }

    public static class ModOverride {

        // All
        private @Nullable ModOverrideState state;
        private @Nullable String fileName;

        // For DISABLED, ENABLED, REMOVED, UPDATED
        private long id = -1;

        // For ADDED
        private @Nullable String sha1; // TODO is this useful?
        private long murmurHash = -1;
        private long curseProject;
        private long curseFile;

        // Gson
        protected ModOverride() {
        }

        public ModOverride(ModOverrideState state, String fileName, long id) {
            this.state = state;
            this.fileName = fileName;
            this.id = id;
        }

        public ModOverride(ModOverrideState state, String fileName, String sha1, long murmurHash, long curseProject, long curseFile) {
            this.state = state;
            this.fileName = fileName;
            this.sha1 = sha1;
            this.curseProject = curseProject;
            this.curseFile = curseFile;
            this.murmurHash = murmurHash;
        }

        public ModOverride(ModOverrideState state, String fileName, long id, String sha1, long murmurHash, long curseProject, long curseFile) {
            this.state = state;
            this.fileName = fileName;
            this.id = id;
            this.sha1 = sha1;
            this.curseProject = curseProject;
            this.curseFile = curseFile;
            this.murmurHash = murmurHash;
        }
        
        public static ModOverride fromApi(ModOverrideState state, ModpackVersionManifest.ModpackFile mod) {
            return new ModOverride(
                state,
                mod.getName(),
                mod.getId()
            );
        }

        // @formatter:off
        public ModOverrideState getState() { return Objects.requireNonNull(state); }
        public String getFileName() { return Objects.requireNonNull(fileName); }
        public long getId() { return id; }
        public String getSha1() { return Objects.requireNonNull(sha1); }
        public long getMurmurHash() { return murmurHash; }
        public long getCurseProject() { return curseProject; }
        public long getCurseFile() { return curseFile; }
        public void setState(ModOverrideState state) { this.state = state; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        // @formatter:on


        @Override
        public String toString() {
            return "ModOverride{" +
                "state=" + state +
                ", fileName='" + fileName + '\'' +
                ", id=" + id +
                ", sha1='" + sha1 + '\'' +
                ", curseProject=" + curseProject +
                ", curseFile=" + curseFile +
                '}';
        }
    }

    public enum ModOverrideState {
        /**
         * Mod was distributed as Disabled, but is now enabled.
         */
        ENABLED,
        /**
         * Mod was distributed as Enabled, but is now disabled.
         */
        DISABLED,
        /**
         * Mod was distributed as Enabled, but is now deleted.
         * <p>
         * This _could_ be a mod which was updated, but not via the GUI.
         */
        REMOVED,
        /**
         * Mod was updated from the distribution and is enabled.
         */
        UPDATED_ENABLED,
        /**
         * Mod was updated from the distribution and is disabled.
         */
        UPDATED_DISABLED,
        /**
         * Mod was added after the fact and is enabled.
         */
        ADDED_ENABLED,
        /**
         * Mod was added after the fact and is disabled.
         */
        ADDED_DISABLED,
        ;

        public ModOverrideState toggle() {
            return switch (this) {
                case DISABLED -> ENABLED;
                case ENABLED -> DISABLED;
                case UPDATED_ENABLED -> UPDATED_DISABLED;
                case UPDATED_DISABLED -> UPDATED_ENABLED;
                case ADDED_ENABLED -> ADDED_DISABLED;
                case ADDED_DISABLED -> ADDED_ENABLED;
                case REMOVED -> throw new UnsupportedOperationException("Unable to toggle removed mods.");
            };
        }

        public boolean enabled() {
            return switch(this) {
                case ENABLED, UPDATED_ENABLED, ADDED_ENABLED -> true;
                default -> false;
            };
        }

        public boolean added() {
            return switch(this) {
                case ADDED_ENABLED, ADDED_DISABLED -> true;
                default -> false;
            };
        }

        public boolean updated() {
            return switch(this) {
                case UPDATED_ENABLED, UPDATED_DISABLED -> true;
                default -> false;
            };
        }
    }
}
