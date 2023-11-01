package net.creeperhost.creeperlauncher.data;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.quack.gson.JsonUtils;
import net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Created by covers1624 on 5/9/23.
 */
public class InstanceModifications {

    public static final Gson GSON = new Gson();

    private @Nullable ModpackVersionManifest.Target modLoaderOverride;

    private final List<ModOverride> overrides = new LinkedList<>();

    public static @Nullable InstanceModifications load(Path path) throws IOException, JsonParseException {
        if (!Files.exists(path)) return null;

        return JsonUtils.parse(GSON, path, InstanceModifications.class);
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
        private long id;

        // For ADDED
        private @Nullable String sha1; // TODO is this useful?
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

        public ModOverride(ModOverrideState state, String fileName, String sha1, long curseProject, long curseFile) {
            this.state = state;
            this.fileName = fileName;
            this.sha1 = sha1;
            this.curseProject = curseProject;
            this.curseFile = curseFile;
        }

        // @formatter:off
        public ModOverrideState getState() { return Objects.requireNonNull(state); }
        public String getFileName() { return Objects.requireNonNull(fileName); }
        public long getId() { return id; }
        public String getSha1() { return Objects.requireNonNull(sha1); }
        public long getCurseProject() { return curseProject; }
        public long getCurseFile() { return curseFile; }
        public void setState(ModOverrideState state) { this.state = state; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        // @formatter:on
    }

    public enum ModOverrideState {
        /**
         * Mod was distributed as Disabled, but is now enabled.
         */
        ENABLED(true),
        /**
         * Mod was distributed as Enabled, but is now disabled.
         */
        DISABLED(false),
        /**
         * Mod was distributed as Enabled, but is now deleted.
         * <p>
         * This _could_ be a mod which was updated, but not via the GUI.
         */
        REMOVED(false),
        /**
         * Mod was updated from the distribution and is enabled.
         */
        UPDATED_ENABLED(true),
        /**
         * Mod was updated from the distribution and is disabled.
         */
        UPDATED_DISABLED(true),
        /**
         * Mod was added after the fact and is enabled.
         */
        ADDED_ENABLED(true, true),
        /**
         * Mod was added after the fact and is disabled.
         */
        ADDED_DISABLED(false, true),
        ;

        public final boolean enabled;
        public final boolean added;

        ModOverrideState(boolean enabled) {
            this(enabled, false);
        }

        ModOverrideState(boolean enabled, boolean added) {
            this.enabled = enabled;
            this.added = added;
        }

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
    }
}
