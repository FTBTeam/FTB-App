package net.creeperhost.creeperlauncher.install.tasks.http;

import com.google.common.hash.HashFunction;

import java.nio.file.Path;

public interface IHttpClient
{
    String makeRequest(String url);

    public DownloadedFile doDownload(String url, Path destination, IProgressUpdater progressWatcher, HashFunction hashFunc, long maxSpeed) throws Throwable;
}
