package net.creeperhost.creeperlauncher.install.tasks;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import net.covers1624.quack.io.IOUtils;
import net.covers1624.quack.net.HttpResponseException;
import net.covers1624.quack.util.MultiHasher;
import net.covers1624.quack.util.MultiHasher.HashFunc;
import net.covers1624.quack.util.MultiHasher.HashResult;
import net.covers1624.quack.util.TimeUtils;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.install.FileValidation;
import net.creeperhost.creeperlauncher.pack.CancellationToken;
import net.creeperhost.creeperlauncher.util.SSLUtils;
import net.creeperhost.creeperlauncher.util.X509Formatter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.*;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.security.cert.X509Certificate;
import java.util.*;

import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.net.HttpURLConnection.HTTP_PARTIAL;

/**
 * A task to download a file.
 * <p>
 * Created by covers1624 on 17/11/21.
 */
public class DownloadTask implements Task {

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
    private final boolean tryResumeDownload;

    private DownloadTask(Builder builder) {
        if (builder.urls.isEmpty()) throw new IllegalStateException("URL not set.");
        if (builder.dest == null) throw new IllegalStateException("Dest not set.");

        tries = builder.tries;
        urls = builder.urls;
        dest = builder.dest;
        validation = builder.validation;

        fileLocator = builder.fileLocator;
        tryCompanionHashes = builder.tryCompanionHashes;
        tryResumeDownload = builder.tryResumeDownload
                && validation.expectedSize != -1 && !validation.expectedHashes.isEmpty(); // We must have hashes and file length, we can't validate the file otherwise.
    }

    /**
     * Creates a new {@link Builder} for building a {@link DownloadTask}.
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
                    doRequest(url, Constants.httpClient(), dest, validation, progressListener);
                    success = true;
                    break outer;
                } catch (Throwable ex) {
                    LOGGER.debug("HTTP/2, Download attempt failed. Attempt {}, URL {}.", i, url, ex);
                    downloadAttempts.add(new FailedDownloadAttempt(url, urlIdx, i, ex, SSLUtils.getThreadCertificates()));
                }
            }
            LOGGER.warn("Download failed 3 times. Trying HTTP/1.1");
            for (int i = 0; i < tries; i++) {
                try {
                    doRequest(url, Constants.http1Client(), dest, validation, progressListener);
                    success = true;
                    break outer;
                } catch (Throwable ex) {
                    LOGGER.debug("HTTP/1.1 Download attempt failed. Attempt {}, URL {}.", i, url, ex);
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
                doRequest(url + ext, Constants.httpClient(), dest, DownloadValidation.of().withUseETag(true).withUseOnlyIfModified(true), null);
                fail = null;
                break;
            } catch (Throwable ex) {
                if (ex instanceof HttpResponseException httpEx && httpEx.code == 404) return null; // Resource doesn't exist.
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

    private void doRequest(String url, OkHttpClient httpClient, Path path, DownloadValidation validation, @Nullable TaskProgressListener progressListener) throws IOException {
        Path tempFile = path.resolveSibling("__tmp_" + path.getFileName());
        Path eTagFile = path.resolveSibling(path.getFileName() + ".etag");

        boolean success = false;
        boolean readAnyBytes = false;
        int tries = 0;
        List<Throwable> downloadFailures = null;
        while (tries++ == 0 || !success && readAnyBytes && tries < 10) {
            readAnyBytes = false;
            LOGGER.info("Trying to download from {}..", url);
            if (tries > 1) {
                LOGGER.info(" Resume try {}.", tries);
            }

            LOGGER.info("  Downloading url: {}", url);
            Request.Builder builder = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", Constants.USER_AGENT);

            if (validation.useETag && Files.exists(eTagFile)) {
                try {
                    builder.addHeader("If-None-Match", Files.readString(eTagFile));
                } catch (IllegalArgumentException ignored) {
                    // Thrown if the etag file is corrupt.
                }
            }

            long lastModified = -1;
            if (validation.useOnlyIfModified && Files.exists(path)) {
                lastModified = Files.getLastModifiedTime(path).toMillis();
                builder.addHeader("If-Modified-Since", TimeUtils.FORMAT_RFC1123.format(new Date(lastModified)));
            }

            boolean tempExists = Files.exists(tempFile);
            long existingSize = tempExists ? Files.size(tempFile) : -1;
            boolean tryResume = tryResumeDownload && tempExists;

            if (tryResume) {
                builder.addHeader("Range", "bytes=" + existingSize + "-");
            }

            if (DEBUG) {
                LOGGER.info("Connecting to {}.", url);
            }

            try (Response response = httpClient.newCall(builder.build()).execute()) {
                int code = response.code();
                validation.validateResponseCode(code, response.message());

                Date lastModifiedHeader = response.headers().getDate("Last-Modified");
                if (validation.validateNotModified(url, code, lastModified, lastModifiedHeader)) {
                    LOGGER.info("  File passed ETag/OnlyIfModified checks.");
                    // We validated ETag/OnlyIfModified
                    return;
                }

                ResponseBody body = response.body();
                if (body == null) {
                    throw new IOException("Got empty response body??");
                }

                boolean isPartial = code == HTTP_PARTIAL;

                long totalLen = body.contentLength();
                if (isPartial) {
                    totalLen += existingSize;
                }

                if (progressListener != null) {
                    progressListener.start(totalLen);
                }

                if (DEBUG) {
                    LOGGER.info("Downloading: '{}' Bytes: {}.", url, totalLen);
                    if (isPartial) {
                        LOGGER.info(" Continuing download. Offset: {}", existingSize);
                    }
                }

                Source s = body.source();
                if (progressListener != null) {
                    s = new ProgressSource(s, progressListener);
                }

                boolean deleteFile = false;
                try (Source source = s) {
                    Path output = IOUtils.makeParents(tempFile);
                    try (BufferedSink sink = Okio.buffer(isPartial ? Okio.sink(output, StandardOpenOption.WRITE, StandardOpenOption.APPEND) : Okio.sink(output))) {
                        long read;
                        while (true) {
                            read = source.read(sink.getBuffer(), 8192);
                            readAnyBytes = true;
                            if (read == -1) break;
                            sink.emitCompleteSegments();
                        }
                        success = true;
                    }
                } catch (IOException ex) {
                    // If we aren't in resume mode, or we tried to resume and did not get 206 partial. Just bonk out.
                    if (!tryResumeDownload || !isPartial && tries > 1) {
                        deleteFile = true;
                        throw ex;
                    }
                    success = false;
                    if (downloadFailures == null) {
                        downloadFailures = new ArrayList<>(10);
                    }
                    downloadFailures.add(ex);
                } finally {
                    if (DEBUG) {
                        boolean hitKnownCache = false;
                        String cloudfrontCache = response.header("x-cache");
                        String cloudflareCache = response.header("cf-cache-status");
                        boolean noCacheHeadersFound = cloudflareCache == null && cloudfrontCache == null;
                        
                        if (cloudflareCache != null && Objects.equals(cloudflareCache, "HIT")) {
                            hitKnownCache = true;
                        } else if (cloudfrontCache != null && cloudfrontCache.toLowerCase().contains("hit")) {
                            hitKnownCache = true;
                        }
                        
                        LOGGER.info("Finished[{}]; Success[{}]; Remote Cache[{}]", url, success, noCacheHeadersFound ? "Unknown" : hitKnownCache);
                    }
                    if (success) {
                        Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
                    } else if (deleteFile) {
                        Files.deleteIfExists(tempFile);
                    }
                }
                if (validation.useOnlyIfModified && lastModifiedHeader != null) {
                    Files.setLastModifiedTime(path, FileTime.fromMillis(lastModifiedHeader.getTime()));
                }
                String eTagHeader = response.header("ETag");
                if (validation.useETag && eTagHeader != null) {
                    Files.writeString(IOUtils.makeParents(eTagFile), eTagHeader, StandardCharsets.UTF_8);
                }
            }
        }

        if (!success) {
            IOException ex = new IOException("Failed to download file after 10 resume attempts.");
            for (Throwable failure : downloadFailures) {
                ex.addSuppressed(failure);
            }
            Files.delete(tempFile);
            throw ex;
        }

        validateDownload(url, path, validation);
    }

    private static void validateDownload(String url, Path dest, DownloadValidation validation) throws IOException {
        HashResult result = hash(dest, validation);
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

    @Nullable
    @Deprecated // Might be redundant now, can probably go back to using the interceptor.
    private static HashResult hash(Path dest, DownloadValidation validation) throws IOException {
        if (validation.expectedHashes.isEmpty()) return null;

        MultiHasher hasher = new MultiHasher(validation.expectedHashes.keySet());
        hasher.load(dest);
        return hasher.finish();
    }

    /**
     * Checks if this {@link DownloadTask} would do anything if executed.
     *
     * @return If this {@link DownloadTask} is redundant.
     */
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
            try (Response response = Constants.httpClient().newCall(request).execute()) {
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
     * Converts this {@link DownloadTask} back to {@link Builder}.
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
        private boolean tryResumeDownload;

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

        public Builder withMirrors(List<String> mirrors) {
            urls.addAll(mirrors);
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

        public Builder tryResumeDownload() {
            this.tryResumeDownload = true;
            return this;
        }

        public DownloadTask build() {
            return new DownloadTask(this);
        }
    }

    public interface LocalFileLocator {

        @Nullable
        Path getLocalFile(FileValidation validation, Path dest);

        void onFileDownloaded(FileValidation validation, Path dest);
    }

    /**
     * Validation properties for a {@link DownloadTask}.
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

        protected boolean expectNotModified() {
            return useETag || useOnlyIfModified;
        }

        protected void validateResponseCode(int code, String reasonPhrase) throws HttpResponseException {
            if ((code < 200 || code > 299) && (!expectNotModified() || code != HTTP_NOT_MODIFIED)) {
                throw new HttpResponseException(code, reasonPhrase);
            }
        }

        protected boolean validateNotModified(String url, int code, long lastModifiedDisk, @Nullable Date lastModifiedHeader) {
            boolean timestampNotModified = useOnlyIfModified && lastModifiedHeader != null && lastModifiedDisk >= lastModifiedHeader.getTime();
            boolean notModified = expectNotModified() && code == HTTP_NOT_MODIFIED;
            if (notModified || timestampNotModified) {
                if (DEBUG) {
                    String reason = "";
                    if (code == HTTP_NOT_MODIFIED) {
                        reason += "304 not modified ";
                    }
                    if (timestampNotModified) {
                        reason += "Last-Modified header";
                    }
                    LOGGER.info("Not Modified ({}). Skipping '{}'.", reason.trim(), url);
                }
                return true;
            }
            return false;
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

    private static class ProgressSource extends ForwardingSource {

        private final TaskProgressListener listener;
        private long totalLen;

        public ProgressSource(@NotNull Source delegate, TaskProgressListener listener) {
            super(delegate);
            this.listener = listener;
        }

        @Override
        public long read(@NotNull Buffer sink, long byteCount) throws IOException {
            long len = super.read(sink, byteCount);
            if (len == -1) {
                listener.finish(totalLen);
            } else {
                totalLen += len;
                listener.update(totalLen);
            }
            return len;
        }
    }
}
