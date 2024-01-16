package net.creeperhost.creeperlauncher.instance;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import net.covers1624.quack.collection.ColUtils;
import net.covers1624.quack.collection.FastStream;
import net.covers1624.quack.io.IOUtils;
import net.covers1624.quack.util.HashUtils;
import net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest;
import net.creeperhost.creeperlauncher.install.FileValidation;
import net.creeperhost.creeperlauncher.install.tasks.DownloadTask;
import net.creeperhost.creeperlauncher.pack.Instance;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Created by covers1624 on 13/4/22.
 */
public abstract class InstanceOperation {

    protected final Instance instance;
    protected final ModpackVersionManifest manifest;

    @Nullable
    protected Map<String, IndexedFile> knownFiles;

    protected InstanceOperation(Instance instance, ModpackVersionManifest manifest) {
        this.instance = instance;
        this.manifest = manifest;
    }

    protected Map<String, IndexedFile> getKnownFiles() {
        if (knownFiles == null) {
            knownFiles = computeKnownFiles(manifest);
        }

        return knownFiles;
    }

    protected Map<String, IndexedFile> computeKnownFiles(ModpackVersionManifest manifest) {
        Map<String, IndexedFile> knownFiles = new HashMap<>();
        for (ModpackVersionManifest.ModpackFile file : manifest.getFiles()) {
            if (file.getName().isEmpty() || file.getUrl().isEmpty()) continue; // What??
            if (file.getType().equals("cf-extract")) continue;

            Path path = file.toPath(instance.getDir()).toAbsolutePath().normalize();
            String relPath = instance.getDir().relativize(path).toString().replace('\\', '/');

            boolean isTrackedMod = Instance.isMod(file.getName());
            knownFiles.put(relPath, new IndexedFile(file.getId(), isTrackedMod, relPath, file.getName(), file.getSha1OrNull(), file.getSize()));
        }
        Path cfOverrides = getCFOverridesZip(manifest);
        if (cfOverrides != null) {
            try (FileSystem fs = IOUtils.getJarFileSystem(cfOverrides, true)) {
                Path root = fs.getPath("/overrides/");
                try (Stream<Path> paths = Files.walk(root)) {
                    for (Path path : ColUtils.iterable(paths)) {
                        if (Files.isDirectory(path)) continue;
                        String relPath = root.relativize(path).toString().replace('\\', '/');
                        knownFiles.put(relPath, new IndexedFile(relPath, path.getFileName().toString(), HashUtils.hash(Hashing.sha1(), path), Files.size(path)));
                    }
                }
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to read CurseForge Overrides zip.", ex);
            }
        }
        return knownFiles;
    }

    @Nullable
    protected Path getCFOverridesZip(ModpackVersionManifest manifest) {
        if (manifest.cfExtractOverride != null) {
            return manifest.cfExtractOverride;
        }

        LinkedList<ModpackVersionManifest.ModpackFile> cfExtractEntries = FastStream.of(manifest.getFiles())
                .filter(e -> e.getType().equals("cf-extract"))
                .toLinkedList();

        if (cfExtractEntries.isEmpty()) return null;
        if (cfExtractEntries.size() > 1) throw new IllegalStateException("More than one cf-extract entry found.");

        ModpackVersionManifest.ModpackFile cfExtractEntry = cfExtractEntries.getFirst();
        DownloadTask task = DownloadTask.builder()
                .url(cfExtractEntry.getUrl())
                .withMirrors(cfExtractEntry.getMirror())
                .dest(cfExtractEntry.getCfExtractPath(instance.getDir(), manifest.getName()))
                .withValidation(cfExtractEntry.createValidation().asDownloadValidation())
                .build();
        try {
            task.execute(null, null);
        } catch (Throwable ex) {
            throw new IllegalStateException("Failed to retrieve CurseForge overrides.");
        }

        return task.getDest();
    }

    protected record IndexedFile(long fileId, boolean isTrackedMod, String relPath, String fileName, @Nullable HashCode sha1, long length) {

        public IndexedFile(String path, String fileName, @Nullable HashCode sha1, long length) {
            this(-1, false, path, fileName, sha1, length);
        }

        public FileValidation createValidation() {
            FileValidation validation = FileValidation.of();
            // Not sure if size can ever be -1, but sure.
            if (length != -1) {
                validation = validation.withExpectedSize(length);
            }
            // sha1 might be null
            if (sha1 != null) {
                validation = validation.withHash(Hashing.sha1(), sha1);
            }
            return validation;
        }
    }
}
