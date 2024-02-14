package net.creeperhost.creeperlauncher.instance.importer.providers;

import com.google.gson.JsonElement;
import net.covers1624.quack.collection.FastStream;
import net.creeperhost.creeperlauncher.instance.importer.meta.InstanceSummary;
import net.creeperhost.creeperlauncher.util.FileUtils;
import net.creeperhost.creeperlauncher.util.GsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Properties;

public abstract class PrismLikeProvider extends InstanceProvider {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final String MC_UID = "net.minecraft";
    private static final String FORGE_UID = "net.minecraftforge";
    private static final String NEO_FORGE_UID = "net.neoforged";
    private static final String FABRIC_UID = "net.fabricmc.fabric-loader";
    private static final String QUILT_UID = "org.quiltmc.quilt-loader";

    @Override
    @Nullable
    public InstanceSummary scanForInstance(Path instancePath) {
        JsonElement meta = GsonUtils.parseRawSafe(instancePath.resolve("mmc-pack.json"));
        if (meta == null) return null;
        Properties props = FileUtils.parsePropertiesSafe(instancePath.resolve("instance.cfg"));
        if (props == null) return null;

        JsonElement components = meta.getAsJsonObject().get("components");
        String mcVersion = findComponentVersion(components, MC_UID);
        if (mcVersion == null) return null;

        return new InstanceSummary(
                props.getProperty("name"),
                instancePath.resolve(".minecraft"),
                mcVersion,
                detectModLoader(components),
                props.getProperty("JavaVersion")
        );
    }

    private static @Nullable InstanceSummary.ModLoader detectModLoader(JsonElement components) {
        String forgeVersion = findComponentVersion(components, FORGE_UID);
        if (forgeVersion != null) return new InstanceSummary.ModLoader("forge", forgeVersion);

        String neoForgeVersion = findComponentVersion(components, NEO_FORGE_UID);
        if (neoForgeVersion != null) return new InstanceSummary.ModLoader("neoforge", neoForgeVersion);

        String fabricVersion = findComponentVersion(components, FABRIC_UID);
        if (fabricVersion != null) return new InstanceSummary.ModLoader("fabric", fabricVersion);

        String quiltVersion = findComponentVersion(components, QUILT_UID);
        if (quiltVersion != null) return new InstanceSummary.ModLoader("quilt", quiltVersion);

        return null;
    }

    private static @Nullable String findComponentVersion(@Nullable JsonElement components, String uid) {
        if (components == null || !components.isJsonArray()) return null;

        return FastStream.of(components.getAsJsonArray())
                .map(JsonElement::getAsJsonObject)
                .filter(e -> e.get("uid").getAsString().equals(uid))
                .map(e -> e.get("version").getAsString())
                .firstOrDefault();
    }
}
