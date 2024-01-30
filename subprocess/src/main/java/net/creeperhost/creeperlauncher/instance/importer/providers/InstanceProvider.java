package net.creeperhost.creeperlauncher.instance.importer.providers;

import com.google.gson.JsonParseException;
import net.covers1624.quack.collection.ColUtils;
import net.covers1624.quack.collection.FastStream;
import net.creeperhost.creeperlauncher.api.handlers.instances.InstalledInstancesHandler;
import net.creeperhost.creeperlauncher.data.modpack.ModpackManifest;
import net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest;
import net.creeperhost.creeperlauncher.instance.importer.meta.InstanceSummary;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;

public abstract class InstanceProvider {

    /**
     * Called to scan a suspected instance folder.
     *
     * @param instancePath The path to scan.
     * @return The scanned instance result.
     */
    @Nullable
    public abstract InstanceSummary scanForInstance(Path instancePath);

    public abstract InstalledInstancesHandler.SugaredInstanceJson importInstance(Path instancePath) throws ImportException;

    protected @Nullable Pair<ModpackManifest, ModpackVersionManifest> findVersionManifest(String type, String version) throws ImportException {
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
