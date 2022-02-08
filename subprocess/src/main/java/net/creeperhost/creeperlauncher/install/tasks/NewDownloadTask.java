package net.creeperhost.creeperlauncher.install.tasks;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import net.covers1624.quack.io.IOUtils;
import net.covers1624.quack.net.okhttp.OkHttpDownloadAction;
import net.covers1624.quack.util.MultiHasher;
import net.covers1624.quack.util.MultiHasher.HashFunc;
import net.covers1624.quack.util.MultiHasher.HashResult;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.install.FileValidation;
import net.creeperhost.creeperlauncher.pack.CancellationToken;
import net.creeperhost.creeperlauncher.util.QuackProgressAdapter;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Throttler;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * A task to download a file.
 * <p>
 * Created by covers1624 on 17/11/21.
 */
public class NewDownloadTask implements Task<Path> {

    private static final boolean DEBUG = Boolean.getBoolean("DownloadTask.debug");
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int DEFAULT_NUM_TRIES = 3;

    private final int tries;
    private final String url;
    private final Path dest;
    private final DownloadValidation validation;

    @Nullable
    private final LocalFileLocator fileLocator;

    private NewDownloadTask(int tries, String url, Path dest, DownloadValidation validation, @Nullable LocalFileLocator fileLocator) {
        this.tries = tries;
        this.url = url;
        this.dest = dest;
        this.validation = validation;

        this.fileLocator = fileLocator;
    }

    /**
     * Creates a new {@link Builder} for building a {@link NewDownloadTask}.
     *
     * @return The {@link Builder}.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void execute(@Nullable CancellationToken token, @Nullable TaskProgressListener progressListener) throws IOException {
        LOGGER.info("Downloading file {}..", dest);
        if (Files.exists(dest) && validation.validate(dest)) {
            LOGGER.info(" File validated.");
            // Validated, do nothing.
            return;
        }

        if (fileLocator != null) {
            Path localPath = fileLocator.getLocalFile(url, validation, dest);
            if (localPath != null && Files.exists(localPath)) {
                LOGGER.info(" File existed locally.");
                Files.copy(localPath, IOUtils.makeParents(dest), StandardCopyOption.REPLACE_EXISTING);
                if (progressListener != null) {
                    long len = Files.size(dest);
                    progressListener.start(len);
                    progressListener.finish(len);
                }
                return;
            }
        }

        Throwable fail = null;
        for (int i = 0; i < tries; i++) {
            try {
                doRequest(progressListener);
                fail = null;
                break;
            } catch (Throwable ex) {
                if (fail != null) {
                    fail.addSuppressed(ex);
                } else {
                    fail = ex;
                }
            }
        }
        if (fail != null) {
            throw new IOException("Download task failed.", fail);
        }

        LOGGER.info("  File downloaded.");

        if (fileLocator != null) {
            fileLocator.onFileDownloaded(url, validation, dest);
        }
    }

    private void doRequest(@Nullable TaskProgressListener progressListener) throws IOException {
        OkHttpDownloadAction action = new OkHttpDownloadAction()
                .setClient(Constants.OK_HTTP_CLIENT)
                .setUrl(url)
                .setDest(dest)
                .setUseETag(validation.useETag)
                .setOnlyIfModified(validation.useOnlyIfModified)
                .addTag(Throttler.class, Constants.getGlobalThrottler());

        if (progressListener != null) {
            action.setDownloadListener(new QuackProgressAdapter(progressListener));
        }
        if (DEBUG) {
            action.setQuiet(false);
        }

        LOGGER.info(" Trying to download from {}..", url);
        action.execute();

        if (action.isUpToDate()) {
            LOGGER.info("  File passed ETag/OnlyIfModified checks.");
            // We validated ETag/OnlyIfModified
            return;
        }

        MultiHasher hashRequest = null;
        if (!validation.expectedHashes.isEmpty()) {
            hashRequest = new MultiHasher(validation.expectedHashes.keySet());
            hashRequest.load(dest);
        }

        HashResult result = hashRequest != null ? hashRequest.finish() : null;

        if (!validation.validate(dest, result)) {
            // Validate will return false when both expectedSize and expectedHash are missing.
            if (validation.expectedSize != -1) {
                long size = Files.size(dest);
                if (validation.expectedSize != size) {
                    throw new IOException("Downloaded size of file '" + url + "' does not match expected. Expected: " + validation.expectedSize + " Got: " + size);
                }
            }
            if (!validation.expectedHashes.isEmpty() && result != null) {
                for (Map.Entry<HashFunc, HashCode> entry : validation.expectedHashes.entrySet()) {
                    HashCode got = result.get(entry.getKey());
                    if (!entry.getValue().equals(got)) {
                        throw new IOException("Downloaded hash of file '" + url + "' does not match expected. Expected: " + entry.getValue() + " Got: " + got);
                    }
                }
            }
        }
    }

    @Override
    public boolean isRedundant() {
        try {
            // Task is redundant if the file exists, and we can validate it.
            return Files.exists(dest) && validation.validate(dest);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public Path getResult() {
        return dest;
    }

    //@formatter:off
    public String getUrl() { return url; }
    public Path getDest() { return dest; }
    public DownloadValidation getValidation() { return validation; }
    @Nullable public LocalFileLocator getFileLocator() { return fileLocator; }
    //@formatter:on

    public static long getContentLength(String url) {
        Request request = new Request.Builder()
                .head()
                .url(url)
                .build();
        try (Response response = Constants.OK_HTTP_CLIENT.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (body != null) return body.contentLength();

            return NumberUtils.toInt(response.header("Content-Length"));
        } catch (IOException e) {
            LOGGER.error("Could not perform a HEAD request to {}", url);
        }
        return 0;
    }

    /**
     * Converts this {@link NewDownloadTask} back to {@link Builder}.
     *
     * @return The {@link Builder}.
     */
    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.url = url;
        builder.dest = dest;
        builder.validation = validation;
        builder.fileLocator = fileLocator;
        return builder;
    }

    // TODO Future improvements here would be to mirror DownloadValidation methods for fluency.
    public static class Builder {

        private int tries = DEFAULT_NUM_TRIES;
        @Nullable
        private String url;
        @Nullable
        private Path dest;

        private DownloadValidation validation = DownloadValidation.of();
        @Nullable
        private LocalFileLocator fileLocator;

        private Builder() { }

        public Builder tries(int tries) {
            this.tries = tries;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder dest(Path dest) {
            this.dest = dest;
            return this;
        }

        public Builder withValidation(DownloadValidation validation) {
            this.validation = validation;
            return this;
        }

        public Builder withFileLocator(LocalFileLocator fileLocator) {
            this.fileLocator = fileLocator;
            return this;
        }

        public NewDownloadTask build() {
            if (url == null) throw new IllegalStateException("URL not set.");
            if (dest == null) throw new IllegalStateException("Dest not set.");

            return new NewDownloadTask(tries, url, dest, validation, fileLocator);
        }
    }

    public interface LocalFileLocator {

        @Nullable
        Path getLocalFile(String url, FileValidation validation, Path dest);

        void onFileDownloaded(String url, FileValidation validation, Path dest);
    }

    /**
     * Validation properties for a {@link NewDownloadTask}.
     * Extension of {@link FileValidation}.
     */
    public static class DownloadValidation extends FileValidation {

        public final boolean useETag;
        public final boolean useOnlyIfModified;

        private DownloadValidation(long expectedSize, Map<HashFunc, HashCode> expectedHashes, boolean useETag, boolean useOnlyIfModified) {
            super(expectedSize, expectedHashes);
            this.useETag = useETag;
            this.useOnlyIfModified = useOnlyIfModified;
        }

        /**
         * Create a new blank {@link DownloadValidation}.
         *
         * @return The new {@link DownloadValidation}.
         */
        public static DownloadValidation of() {
            return new DownloadValidation(-1, Map.of(), false, false);
        }

        /**
         * Creates a new {@link DownloadValidation} from the provided {@link FileValidation}.
         *
         * @param other The {@link FileValidation}.
         * @return The new {@link DownloadValidation}.
         */
        public static DownloadValidation of(FileValidation other) {
            return new DownloadValidation(other.expectedSize, other.expectedHashes, false, false);
        }

        /**
         * Creates a copy of this {@link DownloadValidation} with the provided expected size.
         *
         * @param expectedSize The expected size. <code>-1</code> to disable.
         * @return The new {@link DownloadValidation}.
         */
        public DownloadValidation withExpectedSize(@Range (from = -1, to = Long.MAX_VALUE) long expectedSize) {
            return new DownloadValidation(expectedSize, expectedHashes, useETag, useOnlyIfModified);
        }

        /**
         * Creates a copy of this {@link DownloadValidation} with the provided hash validation.
         *
         * @param hashFunc The {@link HashFunction}.
         * @param hashCode The {@link HashCode} for this function.
         * @return The new {@link DownloadValidation}.
         */
        public DownloadValidation withHash(HashFunction hashFunc, HashCode hashCode) {
            Map<HashFunc, HashCode> expectedHashes = new HashMap<>(this.expectedHashes);
            HashCode existing = expectedHashes.put(HashFunc.find(hashFunc), hashCode);
            if (existing != null) throw new IllegalStateException("HashCode for given HashFunction already set.");

            return new DownloadValidation(expectedSize, expectedHashes, useETag, useOnlyIfModified);
        }

        /**
         * Creates a copy of this {@link DownloadValidation} which will use <code>ETag</code> and
         * <code>If-None-Match</code> Http headers.
         *
         * @param useETag If ETag validation should be used.
         * @return The new {@link DownloadValidation}.
         */
        public DownloadValidation withUseETag(boolean useETag) {
            return new DownloadValidation(expectedSize, expectedHashes, useETag, useOnlyIfModified);
        }

        /**
         * Creates a copy of this {@link DownloadValidation} which will use <code>If-Modified-Since</code>
         * Http headers.
         *
         * @param useOnlyIfModified If <code>If-Modified-Since</code> validation should be used.
         * @return The new {@link DownloadValidation}.
         */
        public DownloadValidation withUseOnlyIfModified(boolean useOnlyIfModified) {
            return new DownloadValidation(expectedSize, expectedHashes, useETag, useOnlyIfModified);
        }
    }

}
