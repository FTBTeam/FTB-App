package net.creeperhost.creeperlauncher.install.tasks;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import net.covers1624.quack.io.IOUtils;
import net.covers1624.quack.net.HttpResponseException;
import net.covers1624.quack.net.okhttp.OkHttpDownloadAction;
import net.covers1624.quack.util.MultiHasher;
import net.covers1624.quack.util.MultiHasher.HashFunc;
import net.covers1624.quack.util.MultiHasher.HashResult;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.install.FileValidation;
import net.creeperhost.creeperlauncher.pack.CancellationToken;
import net.creeperhost.creeperlauncher.util.QuackProgressAdapter;
import net.creeperhost.creeperlauncher.util.SSLUtils;
import net.creeperhost.creeperlauncher.util.X509Formatter;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Throttler;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.cert.X509Certificate;
import java.util.*;

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
    private final List<String> urls;
    private final Path dest;
    private DownloadValidation validation;

    @Nullable
    private final LocalFileLocator fileLocator;
    private final boolean tryCompanionHashes;

    private NewDownloadTask(int tries, List<String> urls, Path dest, DownloadValidation validation, @Nullable LocalFileLocator fileLocator, boolean tryCompanionHashes) {
        this.tries = tries;
        this.urls = urls;
        this.dest = dest;
        this.validation = validation;

        this.fileLocator = fileLocator;
        this.tryCompanionHashes = tryCompanionHashes;
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
        if (Files.exists(dest) && (validation.validate(dest) && !validation.useETag && !validation.useOnlyIfModified)) {
            LOGGER.info(" File validated.");
            // Validated, do nothing.
            return;
        }

        if (fileLocator != null) {
            Path localPath = fileLocator.getLocalFile(validation, dest);
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

        boolean success = false;
        List<FailedDownloadAttempt> downloadAttempts = new LinkedList<>();
        outer:
        for (int urlIdx = 0; urlIdx < urls.size(); urlIdx++) {
            String url = urls.get(urlIdx);
            if (tryCompanionHashes) {
                HashCode sha1 = tryGetCompanionHash(url, HashFunc.SHA1);
                if (sha1 != null) {
                    HashCode validationSha1 = validation.expectedHashes.get(HashFunc.SHA1);
                    if (validationSha1 != null) {
                        if (!sha1.equals(validationSha1)) {
                            LOGGER.error("SHA1 companion to {} does not match Validation. Got: {}, Expected: {}", url, sha1, validationSha1);
                        }
                    } else {
                        validation = validation.withHash(HashFunc.SHA1, sha1);
                    }
                }
            }

            for (int i = 0; i < tries; i++) {
                try {
                    doRequest(url, dest, validation, progressListener);
                    success = true;
                    break outer;
                } catch (Throwable ex) {
                    LOGGER.debug("Download attempt failed. Attempt {}, URL {}.", i, url, ex);
                    downloadAttempts.add(new FailedDownloadAttempt(url, urlIdx, i, ex, SSLUtils.getThreadCertificates()));
                }
            }
        }
        SSLUtils.clearThreadCertificates();
        if (!success) {
            Map<String, List<FailedDownloadAttempt>> groups = new LinkedHashMap<>();
            for (FailedDownloadAttempt attempt : downloadAttempts) {
                groups.computeIfAbsent(attempt.url(), e -> new LinkedList<>()).add(attempt);
            }
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, List<FailedDownloadAttempt>> entry : groups.entrySet()) {
                String url = entry.getKey();
                List<FailedDownloadAttempt> tries = entry.getValue();
                sb.append("Tried URL: '%s' %d times.\n".formatted(url, tries.size()));
                for (int i = 0; i < tries.size(); i++) {
                    sb.append("Try %d: %s".formatted(i, tries.get(i).formatException()));
                }
            }
            throw new DownloadFailedException("Download failed.\n" + sb);
        }

        LOGGER.info("  File downloaded.");

        if (fileLocator != null) {
            fileLocator.onFileDownloaded(validation, dest);
        }
    }

    @Nullable
    private HashCode tryGetCompanionHash(String url, HashFunc func) throws IOException {
        String ext = "." + func.getName().toLowerCase(Locale.ROOT);
        Path dest = this.dest.resolveSibling(this.dest.getFileName().toString() + ext);

        Throwable fail = null;
        for (int i = 0; i < tries; i++) {
            try {
                doRequest(url + ext, dest, DownloadValidation.of().withUseETag(true).withUseOnlyIfModified(true), null);
                fail = null;
                break;
            } catch (Throwable ex) {
                if (ex instanceof HttpResponseException httpEx) {
                    if (httpEx.code == 404) { // Resource doesn't exist.
                        return null;
                    }
                }
                if (fail != null) {
                    fail.addSuppressed(ex);
                } else {
                    fail = ex;
                }
            }
        }
        if (fail != null) {
            LOGGER.error("Failed to retrieve {} companion resource for {}.", func, url, fail);
            return null;
        }
        return HashCode.fromString(Files.readString(dest, StandardCharsets.UTF_8).trim());
    }

    private static void doRequest(String url, Path dest, DownloadValidation validation, @Nullable TaskProgressListener progressListener) throws IOException {
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
            StringBuilder reason = new StringBuilder();
            // Validate will return false when both expectedSize and expectedHash are missing.
            if (validation.expectedSize != -1) {
                long size = Files.size(dest);
                if (validation.expectedSize != size) {
                    reason.append("Expected size: ").append(validation.expectedSize).append(" Got: ").append(size);
                }
            }
            if (!validation.expectedHashes.isEmpty() && result != null) {
                for (Map.Entry<HashFunc, HashCode> entry : validation.expectedHashes.entrySet()) {
                    HashCode got = result.get(entry.getKey());
                    if (!entry.getValue().equals(got)) {
                        if (reason.length() > 0) {
                            reason.append(", ");
                        }
                        reason.append("Expected ").append(entry.getKey().getName()).append(" hash: ").append(entry.getValue()).append(" Got: ").append(got);
                    }
                }
            }
            throw new IOException("Downloaded file '" + url + "'(" + dest + ") failed validation. " + reason);
        }
    }

    @Override
    public boolean isRedundant() {
        try {
            if (!Files.exists(dest)) return false; // File doesn't exist, not redundant.
            if (validation.isRedundant()) return !tryCompanionHashes; // If validation is redundant, we are only redundant if we aren't checking companion hashes.
            if (validation.expectedHashes.isEmpty() && validation.expectedSize == -1) return false; // If validation wouldn't check hashes or size, then we aren't redundant.
            return validation.validate(dest); // Otherwise, we are redundant if validation passes.
        } catch (Throwable ignored) {
            return false;
        }
    }

    //@formatter:off
    @Override public Path getResult() { return dest; }
    public String getUrl() { return urls.get(0); } // TODO REMOVE THIS
    public Path getDest() { return dest; }
    public DownloadValidation getValidation() { return validation; }
    @Nullable public LocalFileLocator getFileLocator() { return fileLocator; }
    //@formatter:on

    // TODO CONVERT TO INSTANCE METHOD WHICH TRIES MIRRORS.
    public static long getContentLength(String url) {
        try {
            Request request = new Request.Builder()
                    .head()
                    .url(url)
                    .build();
            try (Response response = Constants.OK_HTTP_CLIENT.newCall(request).execute()) {
                ResponseBody body = response.body();
                if (body != null) return body.contentLength();

                return NumberUtils.toInt(response.header("Content-Length"));
            }
        } catch (Throwable ex) {
            LOGGER.error("Could not perform a HEAD request to '{}'", url, ex);
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
        builder.urls = new LinkedList<>(urls);
        builder.dest = dest;
        builder.validation = validation;
        builder.fileLocator = fileLocator;
        return builder;
    }

    // TODO Future improvements here would be to mirror DownloadValidation methods for fluency.
    public static class Builder {

        private int tries = DEFAULT_NUM_TRIES;
        private List<String> urls = new LinkedList<>();
        @Nullable
        private Path dest;

        private DownloadValidation validation = DownloadValidation.of();
        @Nullable
        private LocalFileLocator fileLocator;
        private boolean tryCompanionHashes;

        private Builder() { }

        public Builder tries(int tries) {
            this.tries = tries;
            return this;
        }

        public Builder url(String url) {
            assert urls.isEmpty();
            urls.add(url);
            return this;
        }

        public Builder withMirror(String mirror) {
            urls.add(mirror);
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

        public Builder tryCompanionHashes() {
            this.tryCompanionHashes = true;
            return this;
        }

        public NewDownloadTask build() {
            if (urls.isEmpty()) throw new IllegalStateException("URL not set.");
            if (dest == null) throw new IllegalStateException("Dest not set.");

            return new NewDownloadTask(tries, urls, dest, validation, fileLocator, tryCompanionHashes);
        }
    }

    public interface LocalFileLocator {

        @Nullable
        Path getLocalFile(FileValidation validation, Path dest);

        void onFileDownloaded(FileValidation validation, Path dest);
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
            return withHash(Objects.requireNonNull(HashFunc.find(hashFunc)), hashCode);
        }

        /**
         * Creates a copy of this {@link DownloadValidation} with the provided hash validation.
         *
         * @param hashFunc The {@link HashFunction}.
         * @param hashCode The {@link HashCode} for this function.
         * @return The new {@link DownloadValidation}.
         */
        public DownloadValidation withHash(HashFunc hashFunc, HashCode hashCode) {
            Map<HashFunc, HashCode> expectedHashes = new HashMap<>(this.expectedHashes);
            HashCode existing = expectedHashes.put(hashFunc, hashCode);
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

        @Override
        public boolean isRedundant() {
            // We are redundant if super is, and we aren't using etag/onlyIfModified matching.
            return super.isRedundant() && !useETag && !useOnlyIfModified;
        }
    }

    private record FailedDownloadAttempt(String url, int urlIndex, int tryNum, Throwable ex, @Nullable X509Certificate[] certs) {

        public String formatException() {
            StringBuilder sb = new StringBuilder();
            String exception = ExceptionUtils.getStackTrace(ex)
                    .replace("\r\n", "\n")
                    .replace("\n", "\n  ");
            sb.append(exception);
            if (sb.charAt(sb.length() - 1) != '\n') {
                sb.append("\n");
            }
            if (ex instanceof SSLException && certs != null && certs.length != 0) {
                sb.append("Certificate Trace:\n");
                for (int i = 0; i < certs.length; i++) {
                    sb.append("Cert ").append(i).append(": ");
                    if (certs[i] == null) {
                        sb.append("null");
                    } else {
                        sb.append(X509Formatter.printCertificate(certs[i]));
                    }
                }
            }

            return sb.toString();
        }
    }

}
