package net.creeperhost.creeperlauncher.pack;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.io.IOUtils;
import net.covers1624.quack.util.SneakyUtils;
import net.covers1624.quack.util.SneakyUtils.ThrowingConsumer;
import net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest;
import net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest.ModpackFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by covers1624 on 10/2/22.
 */
public class InstanceScanner {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final List<String> SCRIPTS_DIRS = List.of(
            "scripts",
            "kubejs"
    );

    private final Path instanceDir;
    private final ModpackVersionManifest manifest;
    private final Map<String, ModpackFile> modpackFiles = new HashMap<>();

    private final Set<Path> invalidSizedMods = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Path> invalidSizedScripts = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Multimap<String, Path> foundMods = HashMultimap.create();

    public InstanceScanner(Path instanceDir, ModpackVersionManifest manifest) {
        this.instanceDir = instanceDir;
        this.manifest = manifest;
        for (ModpackFile file : manifest.getFiles()) {
            Path path = instanceDir.resolve(file.getPath()).resolve(file.getName());
            modpackFiles.put(instanceDir.relativize(path).toString(), file);
        }
    }

    public boolean isPotentiallyInvalid() {
        boolean potentiallyInvalid = false;
        if (!invalidSizedMods.isEmpty()) {
            String desc = invalidSizedMods.stream().map(e -> e.toAbsolutePath().toString()).collect(Collectors.joining(",", "[", "]"));
            LOGGER.warn("Instance has the following mods which don't appear to be the correct sizes: {}", desc);
            potentiallyInvalid = true;
        }
        if (!invalidSizedScripts.isEmpty()) {
            String desc = invalidSizedScripts.stream().map(e -> e.toAbsolutePath().toString()).collect(Collectors.joining(",", "[", "]"));
            LOGGER.warn("Instance has the following scripts which don't appear to be the correct sizes: {}", desc);
            potentiallyInvalid = true;
        }
        for (Map.Entry<String, Collection<Path>> entry : foundMods.asMap().entrySet()) {
            if (entry.getValue().size() > 1) {
                String desc = entry.getValue().stream().map(e -> e.toAbsolutePath().toString()).collect(Collectors.joining(",", "[", "]"));
                LOGGER.info("Modid '{}' found in more than one Mod jar: {}", entry.getKey(), desc);
                potentiallyInvalid = true;
            }
        }
        return potentiallyInvalid;
    }

    public void scan() {
        Path modsDir = instanceDir.resolve("mods");

        try {
            parallelWalk(modsDir, false, file -> {
                String fileName = file.getFileName().toString();
                if (!fileName.endsWith(".jar") && !fileName.endsWith(".zip")) return;
                String relPath = instanceDir.relativize(file).toString();
                ModpackFile modpackFile = modpackFiles.get(relPath);
                if (modpackFile != null && modpackFile.getSize() != 0 && modpackFile.getSize() != Files.size(file)) {
                    invalidSizedMods.add(file);
                    return;
                }

                investigateMod(file);
            });
            for (String dirName : SCRIPTS_DIRS) {
                Path dir = instanceDir.resolve(dirName);
                if (Files.notExists(dir)) continue;

                parallelWalk(dir, true, file -> {
                    String relPath = instanceDir.relativize(file).toString();
                    ModpackFile modpackFile = modpackFiles.get(relPath);
                    if (modpackFile == null) return;
                    if (modpackFile.getSize() != 0 && modpackFile.getSize() != Files.size(file)) {
                        invalidSizedScripts.add(file);
                    }
                });
            }
        } catch (IOException ex) {
            LOGGER.error("Failed to scan.", ex);
        }
    }

    private void investigateMod(Path mod) {
        try (FileSystem fs = IOUtils.getJarFileSystem(mod, true)) {
            Path modsToml = fs.getPath("/META-INF/mods.toml");
            Path mcmodInfo = fs.getPath("/mcmod.info");
            Path fabricModJson = fs.getPath("/fabric.mod.json");
            if (Files.exists(modsToml)) {
                handleModsToml(mod, modsToml);
            } else if (Files.exists(mcmodInfo)) {
                handleMCModInfo(mod, mcmodInfo);
            } else if (Files.exists(fabricModJson)) {
                handleFabricModJson(mod, fabricModJson);
            }
        } catch (Throwable ex) {
            LOGGER.error("Failed to scan mod '{}'.", mod, ex);
        }
    }

    private void handleModsToml(Path mod, Path path) throws IOException {
        try (FileConfig config = FileConfig.builder(path).build()) {
            config.load();
            List<UnmodifiableConfig> modConfigs = config.get("mods");
            if (modConfigs == null) return;

            for (UnmodifiableConfig modConfig : modConfigs) {
                String modId = modConfig.get("modId");
                if (modId == null) continue;

                foundMods.put(modId, mod);
            }
        }
    }

    private void handleMCModInfo(Path mod, Path path) throws IOException {
        JsonElement element = JsonUtils.parseRaw(path);
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement elm : array) {
                consumeMCModInfoEntry(mod, elm.getAsJsonObject());
            }
        } else {
            consumeMCModInfoEntry(mod, element.getAsJsonObject());
        }
    }

    private void consumeMCModInfoEntry(Path mod, JsonObject obj) {
        JsonPrimitive primitive = JsonUtils.getAsPrimitiveOrNull(obj, "modid");
        if (primitive != null && primitive.isString()) {
            foundMods.put(primitive.getAsString(), mod);
        }
    }

    private void handleFabricModJson(Path mod, Path json) throws IOException {
        JsonObject obj = JsonUtils.parseRaw(json).getAsJsonObject();
        foundMods.put(JsonUtils.getString(obj, "id"), mod);
    }

    private static void parallelWalk(Path dir, boolean nested, ThrowingConsumer<Path, Throwable> consumer) throws IOException {
        try (Stream<Path> files = nested ? Files.walk(dir) : Files.list(dir)) {
            files.parallel()
                    .filter(Files::isRegularFile)
                    .forEach(SneakyUtils.sneak(consumer));
        }
    }
}
