package net.creeperhost.creeperlauncher.install.tasks.http;

import com.google.common.hash.HashFunction;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * Abstract implementation of a Http client.
 */
public interface IHttpClient {

    /**
     * Download the given URL to the specified destination.
     *
     * @param url             The URL to download.
     * @param destination     The destination to put the file.
     * @param progressWatcher The progress watcher. Use {@link IProgressUpdater#NONE} if you don't care.
     * @param hashFunc        The hashing function to use.
     * @param maxSpeed        The Maximum speed to download at. <code>0</code> for infinite.
     * @return The Downloaded file.
     * @throws Throwable If an error occurs whilst downloading.
     */
    DownloadedFile doDownload(String url, Path destination, @Nullable IProgressUpdater progressWatcher, @Nullable HashFunction hashFunc, long maxSpeed) throws Throwable;
}
