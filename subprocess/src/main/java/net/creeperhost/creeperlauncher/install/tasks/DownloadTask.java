package net.creeperhost.creeperlauncher.install.tasks;

import com.google.common.hash.HashCode;
import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.IntegrityCheckException;
import net.creeperhost.creeperlauncher.api.DownloadableFile;
import net.creeperhost.creeperlauncher.install.tasks.http.IProgressUpdater;
import net.creeperhost.creeperlauncher.util.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.*;

public class DownloadTask implements IInstallTask<Void>
{
    private static final Logger LOGGER = LogManager.getLogger();

    private final Path destination;
    static int nThreads = Integer.parseInt(Settings.settings.computeIfAbsent("threadLimit", Settings::getDefaultThreadLimit));
    public static final Executor threadPool = new ThreadPoolExecutor(nThreads, nThreads, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    private int tries = 0;
    private final DownloadableFile file;
    private final IProgressUpdater watcher;
    private static final IProgressUpdater OLD_PROGRESS_UPDATER = (downloaded, delta, total, done) -> FTBModPackInstallerTask.currentBytes.addAndGet(delta);

    public DownloadTask(DownloadableFile file, Path destination) {
        this(file, destination, OLD_PROGRESS_UPDATER);
    }

    public DownloadTask(DownloadableFile file, Path destination, IProgressUpdater watcher)
    {
        this.watcher = watcher;
        this.file = file;
        this.destination = destination;
    }

    @Override
    public CompletableFuture<Void> execute()
    {
        return CompletableFuture.runAsync(() ->
        {
            boolean complete = false;
            FTBModPackInstallerTask.batchedFiles.put(file.getId(), "downloaded");
            while (!complete && tries < 3)
            {
                try
                {
                    ++tries;
                    file.prepare();
                    complete = true;
                } catch (SocketTimeoutException err)
                {
                    if (tries == 3)
                    {
                        throw new IntegrityCheckException(err.getMessage(), -2, null, Collections.emptyList(), 0, 0, file.getUrl(), destination);
                    }
                } catch (IOException e)
                {
                    LOGGER.error("Unable to download {}", file.getName(), e);
                    return;
                }
            }
            complete = false;
            tries = 0;
            while (!complete && tries < 3) {
                if (tries == 0) {
                    if(file.getExpectedSha1() != null && file.getExpectedSha1().size() > 0) {
                        for (HashCode checksum : file.getExpectedSha1()) {
                            if(checksum == null) continue;
                            if(CreeperLauncher.localCache != null && CreeperLauncher.localCache.get(checksum) != null) {
                                Path cachePath = CreeperLauncher.localCache.get(checksum);
                                if (cachePath != null) {
                                    try {
                                        FileUtils.createDirectories(destination.toAbsolutePath().getParent());
                                        Files.copy(cachePath, destination);
                                        watcher.update(0, Files.size(cachePath), 0, true);
                                        FTBModPackInstallerTask.batchedFiles.put(file.getId(), "downloaded");
                                        complete = true;
                                        break;
                                    } catch (IOException e) {
                                        LOGGER.warn("Failed to copy existing file from cache.", e);
                                    }
                                }
                            }
                        }
                    }
                }
                if (!complete)
                {
                    try
                    {
                        ++tries;
                        file.download(destination, false, false, watcher);
                        file.validate(true, true);
                        file.finito();
                        try
                        {
                            CreeperLauncher.localCache.put(file.getLocalFile(), file.getSha1());
                        } catch (Exception err)
                        {
                            LOGGER.error("Error whilst adding to cache: ", err);
                        }
                        FTBModPackInstallerTask.batchedFiles.put(file.getId(), "downloaded");
                        complete = true;
                    } catch (Throwable e)
                    {
                        if (tries == 3)
                        {
                            IntegrityCheckException thrown;
                            if (e instanceof IntegrityCheckException)
                            {
                                LOGGER.debug("Integrity error whilst getting file: ", e);
                                thrown = (IntegrityCheckException)e;
                            } else
                            {
                                thrown = new IntegrityCheckException(e, -1, null, Collections.emptyList(), 0, 0, file.getUrl(), destination);// TODO: make this better
                                LOGGER.debug("Unknown error whilst getting file: ", thrown);
                            }
                            if(Settings.settings.getOrDefault("unforgiving", "false").equals("true"))
                            {
                                throw thrown;
                            }
                        }
                    }
                }
            }
        }, threadPool);
    }

    @Override
    public Double getProgress()
    {
        if (FTBModPackInstallerTask.currentBytes.get() == 0 || FTBModPackInstallerTask.overallBytes.get() == 0) return 0.00d;
        double initPercent = FTBModPackInstallerTask.currentBytes.get() / (double) FTBModPackInstallerTask.overallBytes.get();
        double returnVal = Math.round((initPercent * 100d) * 100d) / 100d;
        return Math.min(returnVal, 100.00d);
    }
}
