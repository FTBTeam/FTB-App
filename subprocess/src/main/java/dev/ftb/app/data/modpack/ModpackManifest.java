package dev.ftb.app.data.modpack;

import com.google.common.hash.HashCode;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import dev.ftb.app.Constants;
import dev.ftb.app.util.ModpackApiUtils;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.quack.gson.HashCodeAdapter;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.net.DownloadAction;
import net.covers1624.quack.net.okhttp.OkHttpDownloadAction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

import static java.util.Objects.requireNonNull;

@SuppressWarnings ("FieldMayBeFinal") // Non-Final for Gson.
public class ModpackManifest {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new Gson();

    @Nullable
    private String status;

    private long id;
    @Nullable
    private String name;
    @Nullable
    private String description;
    @Nullable
    private String synopsis;
    private List<Author> authors = new LinkedList<>();
    private List<Art> art = new LinkedList<>();
    private List<Version> versions = new LinkedList<>();

    @Nullable
    public static ModpackManifest queryManifest(long packId, boolean isPrivate, byte packType) throws IOException, JsonParseException {
        String url = ModpackApiUtils.getModpacksEndpoint(isPrivate, packType) + packId;
        LOGGER.info("Querying Modpack version manifest: {}", url);
        StringWriter sw = new StringWriter();
        DownloadAction action = new OkHttpDownloadAction()
                .setClient(Constants.httpClient())
                .setUserAgent(Constants.USER_AGENT)
                .setUrl(url)
                .setDest(sw);
        
        ModpackApiUtils.injectBearerHeader(action);
        
        action.execute();

        ModpackManifest manifest = JsonUtils.parse(GSON, sw.toString(), ModpackManifest.class);
        if (manifest.getStatus().equals("error")) {
            LOGGER.warn("Query failed. Got: " + sw);
            return null;
        }

        return manifest;
    }

    public static ModpackManifest fakeManifest(String name) {
        ModpackManifest manifest = new ModpackManifest();
        manifest.status = "success";
        manifest.id = -1;
        manifest.name = name;
        manifest.description = "";
        manifest.synopsis = "";
        return manifest;
    }

    @Nullable
    public Art getFirstArt(String type) {
        return FastStream.of(art)
                .filter(e -> e.getType().equals(type))
                .firstOrDefault();
    }

    // @formatter:off
    public String getStatus() { return requireNonNull(status); }
    public long getId() { return id; }
    public String getName() { return requireNonNull(name); }
    public String getDescription() { return requireNonNull(description); }
    public String getSynopsis() { return requireNonNull(synopsis); }
    public List<Author> getAuthors() { return authors; }
    public List<Art> getArt() { return art; }
    // @formatter:on

    public static class Author {

        private long id;

        @Nullable
        private String website;
        @Nullable
        private String name;

        @Nullable
        private String type;

        private long updated;

        // @formatter:off
        public long getId() { return id; }
        public String getWebsite() { return requireNonNull(website); }
        public String getName() { return requireNonNull(name); }
        public String getType() { return requireNonNull(type); }
        public long getUpdated() { return updated; }
        // @formatter:on
    }

    public static class Art {

        private long id;

        private int width;
        private int height;
        @Nullable
        private String url;
        @Nullable
        @JsonAdapter (HashCodeAdapter.class)
        private HashCode sha1;
        private int size;

        @Nullable
        private String type;

        private long updated;

        // @formatter:off
        public long getId() { return id; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public String getUrl() { return requireNonNull(url); }
        public HashCode getSha1() { return requireNonNull(sha1); }
        public int getSize() { return size; }
        public String getType() { return requireNonNull(type); }
        public long getUpdated() { return updated; }
        // @formatter:on
    }

    public static class Version {

        private long id;
        @Nullable
        private String name;
        @Nullable
        private String type;

        private long updated;

        // @formatter:off
        public long getId() { return id; }
        public String getName() { return requireNonNull(name); }
        public String getType() { return requireNonNull(type); }
        public long getUpdated() { return updated; }
        // @formatter:on
    }

}
