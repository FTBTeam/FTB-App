package net.creeperhost.creeperlauncher.install;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import net.covers1624.quack.util.MultiHasher;
import net.covers1624.quack.util.MultiHasher.HashFunc;
import net.creeperhost.creeperlauncher.install.tasks.NewDownloadTask.DownloadValidation;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Helper for validating file hashes and sizes.
 * <p>
 * Created by covers1624 on 19/1/22.
 */
public class FileValidation {

    public final long expectedSize;
    public final Map<HashFunc, HashCode> expectedHashes;

    protected FileValidation(long expectedSize, Map<HashFunc, HashCode> expectedHashes) {
        this.expectedSize = expectedSize;
        this.expectedHashes = expectedHashes;
    }

    /**
     * Create a new blank {@link FileValidation}.
     *
     * @return The new {@link FileValidation}.
     */
    public static FileValidation of() {
        return new FileValidation(-1, Map.of());
    }

    /**
     * Creates a copy of this {@link FileValidation} with the provided expected size.
     *
     * @param expectedSize The expected size. <code>-1</code> to disable.
     * @return The new {@link FileValidation}.
     */
    public FileValidation withExpectedSize(@Range (from = -1, to = Long.MAX_VALUE) long expectedSize) {
        return new FileValidation(expectedSize, expectedHashes);
    }

    /**
     * Creates a copy of this {@link FileValidation} with the provided hash validation.
     *
     * @param hashFunc The {@link HashFunction}.
     * @param hashCode The {@link HashCode} for this function.
     * @return The new {@link FileValidation}.
     */
    public FileValidation withHash(HashFunction hashFunc, HashCode hashCode) {
        return withHash(Objects.requireNonNull(HashFunc.find(hashFunc)), hashCode);
    }

    /**
     * Creates a copy of this {@link FileValidation} with the provided hash validation.
     *
     * @param hashFunc The {@link HashFunction}.
     * @param hashCode The {@link HashCode} for this function.
     * @return The new {@link FileValidation}.
     */
    public FileValidation withHash(HashFunc hashFunc, HashCode hashCode) {
        Map<HashFunc, HashCode> expectedHashes = new HashMap<>(this.expectedHashes);
        HashCode existing = expectedHashes.put(hashFunc, hashCode);
        if (existing != null) throw new IllegalStateException("HashCode for given HashFunction already set.");

        return new FileValidation(expectedSize, expectedHashes);
    }

    /**
     * Create a new {@link DownloadValidation} from this {@link FileValidation}.
     *
     * @return The new {@link DownloadValidation}.
     */
    public DownloadValidation asDownloadValidation() {
        return DownloadValidation.of(this);
    }

    public boolean validate(Path path) throws IOException {
        if (expectedHashes.isEmpty()) return validate(path, null);
        MultiHasher hasher = new MultiHasher(expectedHashes.keySet());
        hasher.load(path);
        return validate(path, hasher.finish());
    }

    public boolean validate(Path path, @Nullable MultiHasher.HashResult hashResult) throws IOException {
        if (expectedSize != -1) {
            long size = Files.size(path);
            if (expectedSize != size) {
                return false;
            }
        }
        if (hashResult != null) {
            for (Map.Entry<HashFunc, HashCode> entry : expectedHashes.entrySet()) {
                if (!entry.getValue().equals(hashResult.get(entry.getKey()))) {
                    return false;
                }
            }
        }
        return true;
    }
}
