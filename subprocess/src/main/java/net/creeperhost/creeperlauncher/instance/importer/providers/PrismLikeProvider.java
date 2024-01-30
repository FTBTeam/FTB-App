package net.creeperhost.creeperlauncher.instance.importer.providers;

import com.google.gson.JsonElement;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.quack.io.CopyingFileVisitor;
import net.creeperhost.creeperlauncher.api.handlers.instances.InstalledInstancesHandler;
import net.creeperhost.creeperlauncher.data.modpack.ModpackManifest;
import net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest;
import net.creeperhost.creeperlauncher.install.InstanceInstaller;
import net.creeperhost.creeperlauncher.install.OperationProgressTracker;
import net.creeperhost.creeperlauncher.instance.importer.meta.InstanceSummary;
import net.creeperhost.creeperlauncher.pack.CancellationToken;
import net.creeperhost.creeperlauncher.pack.Instance;
import net.creeperhost.creeperlauncher.util.FileUtils;
import net.creeperhost.creeperlauncher.util.GsonUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
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
                instancePath,
                mcVersion,
                detectModLoader(components),
                props.getProperty("JavaVersion")
        );
    }

    @Override
    public InstalledInstancesHandler.SugaredInstanceJson importInstance(Path instancePath) throws ImportException {
        InstanceSummary summary = scanForInstance(instancePath);
        if (summary == null) throw new ImportException("Invalid instance. Failed to re-extract data.");
        LOGGER.info("Importing instance in {}", instancePath);

        // TODO wire this in properly.
        OperationProgressTracker tracker = new OperationProgressTracker(
                "import_instance",
                Map.of()
        );

        // Find the vanilla version or mod loader version.
        Pair<ModpackManifest, ModpackVersionManifest> manifests;
        if (summary.modLoader() != null) {
            InstanceSummary.ModLoader ml = summary.modLoader();
            manifests = findVersionManifest(summary.modLoader().type(), summary.modLoader().version());
            if (manifests == null) throw new ImportException("Failed to find version " + ml.version() + " for ModLoader " + ml.type());
        } else {
            manifests = findVersionManifest("minecraft", summary.minecraftVersion());
            if (manifests == null) throw new ImportException("Failed to find vanilla version " + summary.minecraftVersion());
        }
        LOGGER.info("Found pack {} {}", manifests.getLeft().getName(), manifests.getRight().getName());

        // Create the instance.
        Instance instance = new Instance(
                summary.name(),
                null, // TODO
                null, // TODO
                manifests.getLeft(),
                manifests.getRight(),
                summary.minecraftVersion(),
                false, (byte) 0
        );

        LOGGER.info("Copying files..");
        // Copy the files from the instance.
        try {
            Path innerDir = instancePath.resolve(".minecraft");
            Files.walkFileTree(innerDir, new CopyingFileVisitor(innerDir, instance.getDir()));
        } catch (IOException ex) {
            throw new ImportException("Failed to copy instance files.", ex);
        }

        LOGGER.info("Running installers.");
        // Run the installer for vanilla/mod loader and finish the instance.
        try {
            InstanceInstaller installer = new InstanceInstaller(instance, manifests.getRight(), new CancellationToken(), tracker);
            installer.prepare();
            installer.execute();
        } catch (Throwable ex) {
            throw new ImportException("Failed to run installer.", ex);
        }
        LOGGER.info("Done!");
        return new InstalledInstancesHandler.SugaredInstanceJson(instance);
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
