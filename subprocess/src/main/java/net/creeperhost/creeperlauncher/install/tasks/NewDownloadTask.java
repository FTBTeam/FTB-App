package net.creeperhost.creeperlauncher.install.tasks;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import net.covers1624.quack.util.HashUtils;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.install.tasks.http.DownloadedFile;
import net.creeperhost.creeperlauncher.install.tasks.http.IHttpClient;
import net.creeperhost.creeperlauncher.install.tasks.http.IProgressUpdater;
import net.creeperhost.creeperlauncher.install.tasks.http.OkHttpClientImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A task to download a file.
 * <p>
 * Created by covers1624 on 17/11/21.
 */
// TODO, ETag/OnlyIfModified support.
public class NewDownloadTask implements Task<Path> {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final IHttpClient httpClient = new OkHttpClientImpl();

    private final String url;
    private final Path dest;
    private final TaskValidation validation;
    @Nullable
    private final IProgressUpdater progressUpdater;

    private final long id;
    private final String name;
    private final String type;

    public NewDownloadTask(String url, Path dest, TaskValidation validation, @Nullable IProgressUpdater progressUpdater) {
        this(url, dest, validation, progressUpdater, dest.getFileName().toString(), -1, "");
    }

    /**
     * Overload of {@link #NewDownloadTask(String, Path, TaskValidation, IProgressUpdater, String, long, String)},
     * which passes <code>-1</code> to <code>id</code> and <code>""</code> to <code>type</code>.
     */
    public NewDownloadTask(String url, Path dest, TaskValidation validation, String name, @Nullable IProgressUpdater progressUpdater) {
        this(url, dest, validation, progressUpdater, name, -1, "");
    }

    /**
     * A Task that downloads a file.
     *
     * @param url             The URL.
     * @param dest            The Destination for the file.
     * @param validation      The task validation parameters.
     * @param progressUpdater The {@link IProgressUpdater}.
     * @param name            The descriptive name for the file. Usually just the file name.
     * @param id              An additional ID for tracking the file.
     * @param type            The type of this download.
     */
    public NewDownloadTask(String url, Path dest, TaskValidation validation, @Nullable IProgressUpdater progressUpdater, String name, long id, String type) {
        this.url = url;
        this.dest = dest;
        this.validation = validation;
        this.progressUpdater = progressUpdater;

        this.name = name;
        this.id = id;
        this.type = type;
    }

    @Override
    public void execute() throws Throwable {
        if (Files.exists(dest) && validate()) {
            // Validated, do nothing.
            return;
        }

        DownloadedFile downloadedFile = httpClient.doDownload(
                url,
                dest,
                progressUpdater,
                validation.hashFunc,
                Settings.getSpeedLimit()
        );

        if (!validate()) {
            // Validate will return false when both expectedSize and expectedHash are missing.
            if (validation.expectedSize != -1 && validation.expectedSize != downloadedFile.size()) {
                throw new IOException("Downloaded size of file '" + url + "' does not match expected. Expected: " + validation.expectedSize + " Got: " + downloadedFile.size());
            }
            if (validation.expectedHash != null && !validation.expectedHash.equals(downloadedFile.checksum())) {
                throw new IOException("Downloaded hash of file '" + url + "' does not match expected. Expected: " + validation.expectedHash + " Got: " + downloadedFile.checksum());
            }
        }
    }

    @Override
    public boolean isRedundant() {
        try {
            // Task is redundant if the file exists, and we can validate it.
            return Files.exists(dest) && validate();
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public Path getResult() {
        return dest;
    }

    private boolean validate() throws IOException {
        // Both expectedHash and hashFunc must both be missing/present.
        assert (validation.expectedHash == null) == (validation.hashFunc == null);
        // This should only be called when the destination exists.
        assert Files.exists(dest);

        // If the expectedSize and hash are missing, we can't validate.
        if (validation.expectedSize == -1 && validation.expectedHash == null) {
            return false;
        }

        // If expectedSize exists, and doesn't match.
        if (validation.expectedSize != -1 && Files.size(dest) != validation.expectedSize) {
            return false;
        }
        // If we have a hash, and the hash doesn't match.
        if (validation.expectedHash != null && !validation.expectedHash.equals(HashUtils.hash(validation.hashFunc, dest))) {
            return false;
        }

        // Validated! \o/
        return true;
    }

    //@formatter:off
    public String getUrl() { return url; }
    public Path getDest() { return dest; }
    public long getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    //@formatter:on

    /**
     * Validation properties for a {@link NewDownloadTask}.
     */
    @SuppressWarnings ("UnstableApiUsage")
    public static record TaskValidation(long expectedSize, @Nullable HashCode expectedHash, @Nullable HashFunction hashFunc) {

        /**
         * Please use {@link #of(long, HashCode, HashFunction)}.
         */
        @Contract ("_,null,!null->fail;_,!null,null->fail")
        public TaskValidation(@Range (from = -1, to = Long.MAX_VALUE) long expectedSize, @Nullable HashCode expectedHash, @Nullable HashFunction hashFunc) {
            this.expectedSize = expectedSize;
            this.expectedHash = expectedHash;
            this.hashFunc = hashFunc;

            assert (expectedHash == null) == (hashFunc == null);
        }

        /**
         * Create a {@link TaskValidation}.
         * <p>
         * It is expected that this method will be used opposed to directly invoking the record constructor.
         *
         * @param expectedSize The expected size. <code>-1</code> to disable.
         * @param expectedHash The expected {@link HashCode}.<code>null</code> to disable.
         * @param hashFunc     The expected {@link HashFunction}.
         *                     It is expected that if the supplied {@link HashCode} in the <code>expectedHash</code> parameter
         *                     is not null, then this parameter must not be null.
         * @return The {@link TaskValidation}.
         */
        @Contract ("_,null,!null->fail;_,!null,null->fail;_,_,_->new")
        public static TaskValidation of(@Range (from = -1, to = Long.MAX_VALUE) long expectedSize, @Nullable HashCode expectedHash, @Nullable HashFunction hashFunc) {
            return new TaskValidation(expectedSize, expectedHash, hashFunc);
        }

        /**
         * Returns a {@link TaskValidation} which does not validate size or hash.
         *
         * @return The {@link TaskValidation}.
         */
        public static TaskValidation none() {
            return of(-1, null, null);
        }

        /**
         * Returns a {@link TaskValidation} which only validates file sizes.
         *
         * @param expectedSize The expected size. <code>-1</code> to disable.
         * @return The {@link TaskValidation}.
         */
        public static TaskValidation of(@Range (from = -1, to = Long.MAX_VALUE) long expectedSize) {
            return of(expectedSize, null, null);
        }

        /**
         * Returns a {@link TaskValidation} which only validates file hashes.
         *
         * @param expectedHash The expected {@link HashCode}.<code>null</code> to disable.
         * @param hashFunc     The expected {@link HashFunction}.
         *                     It is expected that if the supplied {@link HashCode} in the <code>expectedHash</code> parameter
         *                     is not null, then this parameter must not be null.
         * @return The {@link TaskValidation}.
         */
        @Contract ("null,!null->fail;!null,null->fail;_,_->new")
        public static TaskValidation of(@Nullable HashCode expectedHash, @Nullable HashFunction hashFunc) {
            return of(-1, expectedHash, hashFunc);
        }

        /**
         * Returns a {@link TaskValidation} which will validate only a sha1 hash.
         *
         * @param expectedHash The expected sha1 {@link HashCode}. <code>null</code> to disable.
         * @return The {@link TaskValidation}.
         */
        public static TaskValidation sha1(@Nullable HashCode expectedHash) {
            return of(-1, expectedHash, expectedHash != null ? Hashing.sha1() : null);
        }

        /**
         * Returns a {@link TaskValidation} which will validate file size and a sha1 hash.
         *
         * @param expectedSize The expected size. <code>-1</code> to disable.
         * @param expectedHash The expected sha1 {@link HashCode}. <code>null</code> to disable.
         * @return The {@link TaskValidation}.
         */
        public static TaskValidation sha1(@Range (from = -1, to = Long.MAX_VALUE) long expectedSize, @Nullable HashCode expectedHash) {
            return of(expectedSize, expectedHash, expectedHash != null ? Hashing.sha1() : null);
        }
    }
}
