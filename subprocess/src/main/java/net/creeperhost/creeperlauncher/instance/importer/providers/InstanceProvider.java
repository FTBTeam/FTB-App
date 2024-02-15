package net.creeperhost.creeperlauncher.instance.importer.providers;

import com.google.gson.JsonParseException;
import net.covers1624.quack.collection.ColUtils;
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
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public abstract class InstanceProvider {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Called to scan a suspected instance folder.
     *
     * @param instancePath The path to scan.
     * @return The scanned instance result.
     */
    @Nullable
    public abstract InstanceSummary scanForInstance(Path instancePath);

    public final InstalledInstancesHandler.SugaredInstanceJson importInstance(Path instancePath) throws ImportException {
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
            Files.walkFileTree(summary.instancePath(), new CopyingFileVisitor(summary.instanceDataPath(), instance.getDir()));
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

    private @Nullable Pair<ModpackManifest, ModpackVersionManifest> findVersionManifest(String type, String version) throws ImportException {
        long packId = switch (type) {
            case "minecraft" -> 81;
            case "forge" -> 104;
            case "neoforge" -> 116;
            case "fabric" -> 105;
            default -> throw new IllegalArgumentException("Unknown type. " + type);
        };
        try {
            ModpackManifest modpackManifest = ModpackManifest.queryManifest(packId, false, (byte) 0);

            ModpackManifest.Version v = FastStream.of(modpackManifest.getVersions())
                    .filter(e -> ColUtils.anyMatch(e.getTargets(), t -> t.getName().equals(type) && t.getVersion().equals(version)))
                    .firstOrDefault();
            if (v == null) return null;

            return Pair.of(
                    modpackManifest,
                    ModpackVersionManifest.queryManifest(packId, v.getId(), false, (byte) 0)
            );
        } catch (IOException | JsonParseException ex) {
            throw new ImportException("Exception whilst searching for version manifest.", ex);
        }
    }

    public static class ImportException extends Exception {

        public ImportException(String message) {
            super(message);
        }

        public ImportException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
