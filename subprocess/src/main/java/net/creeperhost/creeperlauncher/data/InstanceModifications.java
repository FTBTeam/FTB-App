package net.creeperhost.creeperlauncher.data;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import net.covers1624.quack.gson.JsonUtils;
import net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by covers1624 on 5/9/23.
 */
public class InstanceModifications {

    public static final Gson GSON = new Gson();

    private @Nullable ModpackVersionManifest.Target modLoaderOverride;

    public static @Nullable InstanceModifications load(Path path) throws IOException, JsonParseException {
        if (!Files.exists(path)) return null;

        return JsonUtils.parse(GSON, path, InstanceModifications.class);
    }

    public static void save(Path path, InstanceModifications modifications) throws IOException {
        JsonUtils.write(GSON, path, modifications);
    }

    public @Nullable ModpackVersionManifest.Target getModLoaderOverride() {
        return modLoaderOverride;
    }

    public void setModLoaderOverride(@Nullable ModpackVersionManifest.Target modLoaderOverride) {
        this.modLoaderOverride = modLoaderOverride;
    }
}
