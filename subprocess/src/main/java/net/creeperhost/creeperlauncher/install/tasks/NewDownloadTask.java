package net.creeperhost.creeperlauncher.install.tasks;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import net.covers1624.quack.io.IOUtils;
import net.covers1624.quack.net.okhttp.MultiHasherInterceptor;
import net.covers1624.quack.net.okhttp.OkHttpDownloadAction;
import net.covers1624.quack.net.okhttp.ThrottlerInterceptor;
import net.covers1624.quack.util.MultiHasher;
import net.covers1624.quack.util.MultiHasher.HashFunc;
import net.covers1624.quack.util.MultiHasher.HashResult;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.install.tasks.http.SimpleCookieJar;
import net.creeperhost.creeperlauncher.util.QuackProgressAdapter;
import okhttp3.OkHttpClient;
import okio.Throttler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * A task to download a file.
 * <p>
 * Created by covers1624 on 17/11/21.
 */
public class NewDownloadTask implements Task<Path> {

    private static final Logger LOGGER = LogManager.getLogger();
    public static final OkHttpClient client = new OkHttpClient.Builder()
            .cookieJar(new SimpleCookieJar())
            .addInterceptor(new ThrottlerInterceptor())
            .addInterceptor(new MultiHasherInterceptor())
            .build();

    private final String url;
    private final Path dest;
    private final DownloadValidation validation;

    private final boolean useCache;
    private final long id;
    private final String name;
    private final String type;

    public NewDownloadTask(String url, Path dest, DownloadValidation validation) {
        this(url, dest, validation, false, dest.getFileName().toString(), -1, "");
    }

    /**
     * Overload of {@link #NewDownloadTask(String, Path, DownloadValidation, boolean, String, long, String)},
     * which passes <code>-1</code> to <code>id</code> and <code>""</code> to <code>type</code>.
     */
    public NewDownloadTask(String url, Path dest, DownloadValidation validation, String name) {
        this(url, dest, validation, false, name, -1, "");
    }

    /**
     * A Task that downloads a file.
     *
     * @param url        The URL.
     * @param dest       The Destination for the file.
     * @param validation The task validation parameters.
     * @param useCache   If this task should use the {@link LocalCache}.
     * @param name       The descriptive name for the file. Usually just the file name.
     * @param id         An additional ID for tracking the file.
     * @param type       The type of this download.
     */
    public NewDownloadTask(String url, Path dest, DownloadValidation validation, boolean useCache, String name, long id, String type) {
        this.url = url;
        this.dest = dest;
        this.validation = validation;

        this.useCache = useCache;
        this.name = name;
        this.id = id;
        this.type = type;
    }

    @Override
    public void execute(@Nullable TaskProgressListener progressListener) throws IOException {
        if (Files.exists(dest) && validation.validate(dest)) {
            // Validated, do nothing.
            return;
        }

        // TODO, SHA1 hardcode..
        if (useCache && validation.expectedHashes.containsKey(HashFunc.SHA1)) {
            Path cachePath = CreeperLauncher.localCache.get(validation.expectedHashes.get(HashFunc.SHA1));
            if (cachePath != null) {
                Files.copy(cachePath, IOUtils.makeParents(dest));
                if (progressListener != null) {
                    long len = Files.size(dest);
                    progressListener.start(len);
                    progressListener.finish(len);
                }
                return;
            }
        }

        MultiHasher hashRequest = null;
        OkHttpDownloadAction action = new OkHttpDownloadAction()
                .setClient(client)
                .setUrl(url)
                .setDest(dest)
                .setUseETag(validation.useETag)
                .setOnlyIfModified(validation.useOnlyIfModified)
                .addTag(Throttler.class, Constants.getGlobalThrottler());
        if (!validation.expectedHashes.isEmpty()) {
            hashRequest = new MultiHasher(validation.expectedHashes.keySet());
            action.addTag(MultiHasher.class, hashRequest);
        }

        if (progressListener != null) {
            action.setDownloadListener(new QuackProgressAdapter(progressListener));
        }

        action.execute();

        if (action.isUpToDate()) {
            // We validated ETag/OnlyIfModified
            return;
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

        // TODO, SHA1 hardcode..
        if (useCache && validation.expectedHashes.containsKey(HashFunc.SHA1)) {
            CreeperLauncher.localCache.put(dest, validation.expectedHashes.get(HashFunc.SHA1));
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
    public long getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    //@formatter:on

    /**
     * Validation properties for a {@link NewDownloadTask}.
     */
    public static record DownloadValidation(long expectedSize, Map<HashFunc, HashCode> expectedHashes, boolean useETag, boolean useOnlyIfModified) {

        /**
         * Create a new blank {@link DownloadValidation}.
         *
         * @return The new {@link DownloadValidation}.
         */
        public static DownloadValidation of() {
            return new DownloadValidation(-1, Map.of(), false, false);
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

        private boolean validate(Path path) throws IOException {
            if (expectedHashes.isEmpty()) return validate(path, null);
            MultiHasher hasher = new MultiHasher(expectedHashes.keySet());
            hasher.load(path);
            return validate(path, hasher.finish());
        }

        private boolean validate(Path path, @Nullable HashResult hashResult) throws IOException {
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

}
