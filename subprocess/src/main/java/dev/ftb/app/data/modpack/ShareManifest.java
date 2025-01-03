package dev.ftb.app.data.modpack;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.net.DownloadAction;
import net.covers1624.quack.net.okhttp.OkHttpDownloadAction;
import dev.ftb.app.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Objects;

/**
 * Created by covers1624 on 1/6/22.
 */
public class ShareManifest {

    private static final Logger LOGGER = LogManager.getLogger();

    public static final Gson GSON = ModpackVersionManifest.GSON;

    private long modpackId;
    @Nullable
    private Type type;
    @Nullable
    private String name;
    @Nullable
    private ModpackVersionManifest versionManifest;

    public ShareManifest() {
    }

    public ShareManifest(long modpackId, Type type, ModpackVersionManifest versionManifest) {
        this.modpackId = modpackId;
        this.type = type;
        this.versionManifest = versionManifest;
    }

    public ShareManifest(String name, Type type, ModpackVersionManifest versionManifest) {
        this.modpackId = -1;
        this.type = type;
        this.name = name;
        this.versionManifest = versionManifest;
    }

    @Nullable
    public static ShareManifest queryManifest(String url) throws JsonParseException {
        LOGGER.info("Querying Share manifest: {}", url);
        try {
            StringWriter sw = new StringWriter();
            DownloadAction action = new OkHttpDownloadAction()
                    .setClient(Constants.httpClient())
                    .setUserAgent(Constants.USER_AGENT)
                    .setUrl(url)
                    .setDest(sw);
            action.execute();

            return JsonUtils.parse(GSON, sw.toString(), ShareManifest.class);
        } catch (IOException ex) {
            LOGGER.warn("Failed to query Share manifest from: {}", url);
            return null;
        }
    }

    // @formatter:off
    public long getModpackId() { return modpackId; }
    public String getName() { return Objects.requireNonNull(name); }
    public Type getType() { return Objects.requireNonNull(type); }
    public ModpackVersionManifest getVersionManifest() { return Objects.requireNonNull(versionManifest); }
    // @formatter:on

    public enum Type {
        PUBLIC,
        PRIVATE,
        CURSE,
        IMPORT,
    }
}
