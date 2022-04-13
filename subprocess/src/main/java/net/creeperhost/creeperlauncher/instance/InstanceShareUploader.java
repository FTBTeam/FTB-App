package net.creeperhost.creeperlauncher.instance;

import com.google.common.collect.Iterables;
import com.google.common.hash.Hashing;
import net.covers1624.quack.collection.ColUtils;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.util.HashUtils;
import net.covers1624.quack.util.MultiHasher;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest;
import net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest.ModpackFile;
import net.creeperhost.creeperlauncher.install.FileValidation;
import net.creeperhost.creeperlauncher.pack.LocalInstance;
import okhttp3.*;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Created by covers1624 on 13/4/22.
 */
public class InstanceShareUploader extends InstanceOperation {

    private static final String TRANSFER_HOST = "http://localhost:8081/";

    // Files known to be internal to the Instance system, they should not be shared.
    private static final Set<String> FORCE_SKIP_FILES = Set.of(
            "instance.json",
            "version.json",
            "folder.jpg"
    );
    private static final Logger LOGGER = LogManager.getLogger();
    private static final boolean DEBUG = Boolean.getBoolean("InstanceShareUploader.debug");

    private final List<IndexedFile> untrackedFiles = new LinkedList<>();
    private final List<IndexedFile> modifiedFiles = new LinkedList<>();
    private final List<IndexedFile> removedFiles = new LinkedList<>();

    public InstanceShareUploader(LocalInstance instance) {
        super(instance, instance.versionManifest);
    }

    public void prepare() {
        locateUntrackedFiles();
        locateModifiedFiles();

        if (DEBUG) {
            LOGGER.info("Found the following removed files:");
            for (IndexedFile removedFile : removedFiles) {
                LOGGER.info("    " + removedFile.path());
            }
            LOGGER.info("Found the following modified files:");
            for (IndexedFile modifiedFile : modifiedFiles) {
                LOGGER.info("    " + modifiedFile.path());
            }
            LOGGER.info("Found the following untrakced files:");
            for (IndexedFile untrackedFile : untrackedFiles) {
                LOGGER.info("    " + untrackedFile.path());
            }
        }
    }

    public String execute() {
        ModpackVersionManifest manifest = this.manifest.copy();
        List<ModpackFile> modpackFiles = manifest.getFiles();

        // TODO generify the outer loop of all of these to a 'FileAction'
        // Apply removals.
        for (IndexedFile removedFile : removedFiles) {
            Iterator<ModpackFile> itr = modpackFiles.iterator();
            while (itr.hasNext()) {
                if (itr.next().instanceRelPath().equals(removedFile.path())) {
                    itr.remove();
                    break;
                }
            }
        }

        // Apply modifications.
        for (IndexedFile modifiedFile : modifiedFiles) {
            assert modifiedFile.sha1() != null;
            for (ModpackFile file : modpackFiles) {
                if (file.instanceRelPath().equals(modifiedFile.path())) {
                    file.setUrl(null); // Marker for importer to calculate from the hash plus the share key.
                    file.setSize(modifiedFile.length());
                    file.setSha1(modifiedFile.sha1());
                    break;
                }
            }
        }

        // Add Untracked files.
        for (IndexedFile file : untrackedFiles) {
            assert file.sha1() != null;

            modpackFiles.add(new ModpackFile(
                    -1,
                    "./" + FilenameUtils.getFullPath(file.path()),
                    FilenameUtils.getName(file.path()),
                    file.sha1(),
                    file.length()
            ));
        }

        Path versionFile;
        try {
            String versionJson = ModpackVersionManifest.GSON.toJson(manifest);
            versionFile = Files.createTempFile("version", ".json");
            Files.writeString(versionFile, versionJson, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to create temp file for version.json");
        }

        String code = uploadFiles(versionFile);
        LOGGER.info("Uploaded instance: {}", code);
        return code;
    }

    // TODO, Slightly different to InstanceInstaller but similar enough that it could go into InstanceOperation with a bit of work
    //  but that is out of scope atm.
    private void locateUntrackedFiles() {
        Map<String, IndexedFile> knownFiles = getKnownFiles();
        try (Stream<Path> files = Files.walk(instance.getDir())) {
            for (Path path : ColUtils.iterable(files)) {
                if (Files.isDirectory(path)) continue; // Skip directories.
                if (FORCE_SKIP_FILES.contains(path.getFileName().toString())) continue; // Skip these files too.
                String relPath = instance.getDir().relativize(path).toString();
                if (knownFiles.containsKey(relPath)) continue; // File is known.

                untrackedFiles.add(new IndexedFile(relPath, HashUtils.hash(Hashing.sha1(), path), Files.size(path)));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("An IO error occurred whilst locating untracked files.", ex);
        }
    }

    private void locateModifiedFiles() {
        Map<String, IndexedFile> knownFiles = getKnownFiles();
        for (Map.Entry<String, IndexedFile> entry : knownFiles.entrySet()) {
            Path file = instance.getDir().resolve(entry.getKey());
            IndexedFile modpackFile = entry.getValue();
            if (Files.notExists(file)) {
                removedFiles.add(modpackFile);
                continue;
            }

            try {
                FileValidation validation = modpackFile.createValidation();
                MultiHasher hasher = new MultiHasher(MultiHasher.HashFunc.SHA1);
                hasher.load(file);
                MultiHasher.HashResult result = hasher.finish();
                if (!validation.validate(file, result)) {
                    modifiedFiles.add(new IndexedFile(
                            modpackFile.path(),
                            result.get(MultiHasher.HashFunc.SHA1),
                            Files.size(file)
                    ));
                }
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to validate file '" + file + "'.", ex);
            }
        }
    }

    private String uploadFiles(Path versionJson) {
        MultipartBody.Builder postBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "version.json", bodyOf(versionJson));
        for (IndexedFile file : Iterables.concat(modifiedFiles, untrackedFiles)) {
            assert file.sha1() != null;
            // TODO assert that hash collisions have the same file length and ignore, else explode.

            postBody.addFormDataPart("file", file.sha1().toString(), bodyOf(instance.getDir().resolve(file.path())));
        }
        Request.Builder requestBuilder = new Request.Builder()
                .url(TRANSFER_HOST)
                .post(postBody.build());
        try (Response response = Constants.OK_HTTP_CLIENT.newCall(requestBuilder.build()).execute()) {
            ResponseBody body = response.body();
            if (!response.isSuccessful()) {
                String bodyStr = body != null ? body.string() : null;
                throw new RuntimeException("Got: " + response.code() + " body: " + bodyStr);
            }
            if (body == null) {
                throw new IllegalStateException("Got empty response body on success??");
            }
            try (BufferedReader reader = new BufferedReader(body.charStream())) {
                String line = reader.readLine().replace(TRANSFER_HOST, "");
                int slash = line.indexOf("/");
                return line.substring(0, slash);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("An IO error occurred whilst uploading pack.", ex);
        }
    }

    private static RequestBody bodyOf(Path file) {
        try {
            String probedType = Files.probeContentType(file);
            MediaType mediaType = probedType != null ? MediaType.parse(probedType) : null;
            return new RequestBody() {
                @Nullable
                @Override
                public MediaType contentType() {
                    return mediaType;
                }

                @Override
                public long contentLength() throws IOException {
                    return Files.size(file);
                }

                @Override
                public void writeTo(@NotNull BufferedSink sink) throws IOException {
                    try (Source source = Okio.source(file)) {
                        sink.writeAll(source);
                    }
                }
            };
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to prepare body.", ex);
        }
    }
}
