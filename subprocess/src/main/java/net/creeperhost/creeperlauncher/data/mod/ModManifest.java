package net.creeperhost.creeperlauncher.data.mod;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.net.DownloadAction;
import net.covers1624.quack.net.okhttp.OkHttpDownloadAction;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.install.FileValidation;
import net.creeperhost.creeperlauncher.install.ModCollector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

/**
 * Created by covers1624 on 30/8/23.
 */
public class ModManifest {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final Gson GSON = new Gson();

    private @Nullable String status;

    private long id;
    private @Nullable String type;

    private @Nullable String name;
    private @Nullable String synopsis;
    private @Nullable String description;

    private long installs;
    private long updated;
    private long refreshed;

    private final List<Art> art = new LinkedList<>();
    private final List<Link> links = new LinkedList<>();
    private final List<Author> authors = new LinkedList<>();
    private final List<Version> versions = new LinkedList<>();

    public static @Nullable ModManifest tryQuery(long id) {
        try {
            return queryManifest(id);
        } catch (IOException | JsonParseException ex) {
            LOGGER.warn("Failed to query mod manifest for {}.", id, ex);
            return null;
        }
    }

    public static ModManifest queryManifest(long id) throws IOException, JsonParseException {
        return queryManifest(String.valueOf(id));
    }

    public static ModManifest queryManifest(String id) throws IOException, JsonParseException {
        StringWriter sw = new StringWriter();
        DownloadAction action = new OkHttpDownloadAction()
                .setClient(Constants.httpClient())
                .setUrl(Constants.getModEndpoint() + id)
                .setDest(sw);
        action.execute();

        return JsonUtils.parse(GSON, sw.toString(), ModManifest.class);
    }

    public @Nullable Version findVersion(long versionId) {
        return FastStream.of(versions)
                .filter(e -> e.id == versionId)
                .firstOrDefault();
    }

    public void visitVersion(ModCollector collector, Version version) {
        collector.addMod(this, version);
        version.visitDependencies(this, collector);
    }

    public @Nullable Version findLatestCompatibleVersion(String modLoader, String mcVersion) {
        return FastStream.of(versions)
                .filter(e -> {
                    Target gameTarget = e.getTarget("game");
                    if (gameTarget == null || !gameTarget.getVersion().equals(mcVersion)) return false;

                    Target loaderTarget = e.getTarget("modloader");
                    if (loaderTarget == null) {
                        // Lots of mods don't have a Forge requirement, as ModLoader selection was introduced after they were added.
                        // By default, we just assume its Forge compatible. If this turns out to be an issue we can perhaps refine it.
                        return modLoader.equals("forge");
                    }
                    return loaderTarget.getName().equals(modLoader);
                })
                .sorted(Comparator.comparingLong(e -> -e.id))
                .firstOrDefault();
    }

    public void visitCompatibleVersion(ModManifest requestedBy, ModCollector collector) {
        Version version = findLatestCompatibleVersion(collector.modLoader(), collector.mcVersion());
        if (version == null) {
            collector.unsatisfiableDependency(requestedBy, this);
            return;
        }

        visitVersion(collector, version);
    }

    // @formatter:off
    public @Nullable String getStatus() { return status; }
    public long getId() { return id; }
    public String getType() { return Objects.requireNonNull(type); }
    public String getName() { return Objects.requireNonNull(name); }
    public String getSynopsis() { return Objects.requireNonNull(synopsis); }
    public String getDescription() { return Objects.requireNonNull(description); }
    public long getInstalls() { return installs; }
    public long getUpdated() { return updated; }
    public long getRefreshed() { return refreshed; }
    public List<Art> getArt() { return art; }
    public List<Link> getLinks() { return links; }
    public List<Author> getAuthors() { return authors; }
    public List<Version> getVersions() { return versions; }
    // @formatter:on

    public static class Art {

        private @Nullable String type;
        private boolean compressed;
        private int width;
        private int height;
        private @Nullable String url;
        private final List<String> mirrors = new LinkedList<>();

        // @formatter:off
        public String getType() { return Objects.requireNonNull(type); }
        public boolean isCompressed() { return compressed; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public String getUrl() { return Objects.requireNonNull(url); }
        public List<String> getMirrors() { return mirrors; }
        // @formatter:on
    }

    public static class Link {

        private long id;
        private @Nullable String name;
        private @Nullable String link;
        private @Nullable String type;

        // @formatter:off
        public long getId() { return id; }
        public String getName() { return Objects.requireNonNull(name); }
        public String getLink() { return Objects.requireNonNull(name); }
        public String getType() { return Objects.requireNonNull(type); }
        // @formatter:on
    }

    public static class Author {

        private long id;
        private @Nullable String website;
        private @Nullable String name;
        private @Nullable String type;
        private long updated;

        // @formatter:off
        public long getId() { return id; }
        public String getWebsite() { return Objects.requireNonNull(website); }
        public String getName() { return Objects.requireNonNull(name); }
        public String getType() { return Objects.requireNonNull(type); }
        public long getUpdated() { return updated; }
        // @formatter:on
    }

    public static class Version {

        private long id;
        private @Nullable String version;
        private @Nullable String name;

        private @Nullable String path;
        private @Nullable String url;
        private final List<String> mirrors = new LinkedList<>();
        private @Nullable String sha1;
        private long size;

        private boolean clientonly;
        private @Nullable String type;

        private final List<Target> targets = new LinkedList<>();
        private final List<Dependency> dependencies = new LinkedList<>();

        // @formatter:off
        public long getId() { return id; }
        public String getVersion() { return Objects.requireNonNull(version); }
        public String getName() { return Objects.requireNonNull(name); }
        public String getPath() { return Objects.requireNonNull(path); }
        public String getUrl() { return Objects.requireNonNull(url); }
        public List<String> getMirrors() { return mirrors; }
        public String getSha1() { return Objects.requireNonNull(sha1); }
        public long getSize() { return size; }
        public boolean isClientonly() { return clientonly; }
        public String getType() { return Objects.requireNonNull(type); }
        public List<Target> getTargets() { return targets; }
        public List<Dependency> getDependencies() { return dependencies; }
        // @formatter:on

        public void visitDependencies(ModManifest mod, ModCollector visitor) {
            for (Dependency dependency : dependencies) {
                dependency.visit(mod, visitor);
            }
        }

        public @Nullable Target getTarget(String target) {
            return FastStream.of(targets)
                    .filter(e -> Objects.equals(e.type, target))
                    .firstOrDefault();
        }

        public FileValidation createValidation() {
            FileValidation validation = FileValidation.of();
            // Not sure if size can ever be -1, but sure.
            if (size != -1) {
                validation = validation.withExpectedSize(size);
            }
            // sha1 might be null
            if (sha1 != null) {
                validation = validation.withHash(Hashing.sha1(), HashCode.fromString(sha1));
            }
            return validation;
        }
    }

    public static class Target {

        private long id;
        private @Nullable String version;
        private @Nullable String name;
        private @Nullable String type;
        private long updated;

        // @formatter:off
        public long getId() { return id; }
        public String getVersion() { return Objects.requireNonNull(version); }
        public String getName() { return Objects.requireNonNull(name); }
        public String getType() { return Objects.requireNonNull(type); }
        public long getUpdated() { return updated; }
        // @formatter:on
    }

    public static class Dependency {

        private long id;
        private boolean required;

        // @formatter:off
        public long getId() { return id; }
        public boolean isRequired() { return required; }
        // @formatter:on

        public void visit(ModManifest mod, ModCollector collector) {
            if (!required) return;

            ModManifest manifest = tryQuery(id);
            if (manifest == null) {
                collector.unavailableDependency(mod, this);
                return;
            }
            manifest.visitCompatibleVersion(mod, collector);
        }
    }
}
