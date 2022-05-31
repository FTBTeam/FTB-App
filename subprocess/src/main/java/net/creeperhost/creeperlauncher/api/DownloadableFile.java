package net.creeperhost.creeperlauncher.api;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.IntegrityCheckException;
import net.creeperhost.creeperlauncher.install.tasks.http.DownloadedFile;
import net.creeperhost.creeperlauncher.install.tasks.http.IHttpClient;
import net.creeperhost.creeperlauncher.install.tasks.http.IProgressUpdater;
import net.creeperhost.creeperlauncher.install.tasks.http.OkHttpClientImpl;
import net.creeperhost.creeperlauncher.util.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DownloadableFile
{
    private static final Logger LOGGER = LogManager.getLogger();

    private final Path path;
    private final String downloadUrl;
    private final List<HashCode> expectedChecksums;
    private long size;
    private final long id;
    private final String name;
    private final String type;
    private boolean hasPrepared;
    private HashCode sha1;
    private Path destination;
    private final IHttpClient client = new OkHttpClientImpl();

    public DownloadableFile(Path path, String url, List<HashCode> acceptedChecksums, long size, long id, String name, String type) {
        this.path = path;
        this.downloadUrl = url;
        this.expectedChecksums = new ArrayList<>(acceptedChecksums);
        this.size = size;
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public void prepare() throws IOException {
        boolean remoteExists = false;
        long remoteSize = 0;
        if (this.downloadUrl.length() > 10) {
            URL url = new URL(downloadUrl.replace(" ", "%20"));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", Constants.USER_AGENT);
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(25000);
            connection.connect();
            //Grab the new origin header
            int tmpContentLength = 0;
            boolean pokeOrigin = true;
            try {
                tmpContentLength = connection.getContentLength();
                if(tmpContentLength > 0)
                {
                    //We've managed to get the content length from the cdn!
                    pokeOrigin = false;
                }
            } catch (Exception ignored)
            {
                pokeOrigin = true;
            }
            if(pokeOrigin) {
                //cdn is not giving us content length due to gzip(?), let's go poke the origin of the files.
                String origin = connection.getHeaderField("origin");
                if (origin != null && origin.length() > 0) {
                    //If we have an origin header, let's grab the file size from the horses mouth
                    connection.disconnect();
                    URL _url = new URL(origin.replace(" ", "%20"));
                    connection = (HttpURLConnection) _url.openConnection();
                    connection.setRequestProperty("User-Agent", Constants.USER_AGENT);
                    connection.setRequestMethod("HEAD");
                    connection.setConnectTimeout(15000);
                    connection.setReadTimeout(25000);
                    connection.connect();
                    tmpContentLength = connection.getContentLength();
                }
            }
            remoteSize = tmpContentLength;
            remoteExists = ((connection.getResponseCode() == 200) && (tmpContentLength >= 0));
            if(!remoteExists)
            {
                if(connection.getResponseCode() == 200)
                {
                    remoteExists = true;
                    LOGGER.warn("{} unable to get content length from HTTP headers!", getName());
                    remoteSize = this.getSize();
                } else {
                    LOGGER.warn("{} error {}: {}!", getName(), connection.getResponseCode(), connection.getResponseMessage());
                }
            }
            connection.disconnect();
        }
        if (this.getSize() != remoteSize)
        {
            if (this.getSize() > 0)
            {
//                FTBModPackInstallerTask.overallBytes.set(FTBModPackInstallerTask.overallBytes.get() - this.getSize());
                LOGGER.warn("{} size expected does not match remote file size. File size updated.", getName());
            }
            this.size = remoteSize;
//            FTBModPackInstallerTask.overallBytes.addAndGet(this.getSize());
        }
        if (!remoteExists)
        {
            throw new FileNotFoundException("File does not exist at URL.");
        }
        this.hasPrepared = true;
    }

    public void download(Path destination, boolean OverwriteOnExist, boolean FailOnExist, IProgressUpdater watcher) throws Throwable
    {
        if (!hasPrepared)
            throw new UnsupportedOperationException("Unable to download file that has not been prepared.");
        if (Files.exists(destination))
        {

            if (!OverwriteOnExist)
            {
                if (FailOnExist)
                {
                    throw new FileAlreadyExistsException("Cannot download to file which already exists.");
                } else
                {

                    LOGGER.warn("{} already exists.", getName());
                }
            }
        }
        FileUtils.createDirectories(destination.getParent());

        DownloadedFile send = client.doDownload(
                downloadUrl,
                destination,
                watcher,
                Hashing.sha1(),
                Settings.getSpeedLimit() * 1000L); // not really async - our client will run async things on same thread. bit of a hack, but async just froze.
        Path body = send.destination();
        sha1 = send.checksum();

        this.destination = body;
    }

    public Path getLocalFile()
    {
        return destination;
    }

    public void validate(boolean FailOnChecksum, boolean FailOnFileSize) throws IntegrityCheckException, FileNotFoundException
    {
        if (Files.notExists(destination)) throw new FileNotFoundException("File not saved.");

        long dstSize = 0;
        try {
            dstSize = Files.size(destination);
        } catch (IOException ignored) {
            LOGGER.warn("Failed to get size of file: {}", destination);
        }
        if (!expectedChecksums.isEmpty() && sha1 != null) {
            if (!expectedChecksums.contains(sha1)) {
                if (FailOnChecksum) {
                    throw new IntegrityCheckException("SHA1 checksum does not match.", -1, sha1, expectedChecksums, dstSize, size, downloadUrl, path);
                } else {
                    LOGGER.warn("{}'s SHA1 checksum failed.", getName());
                }
            }
        }
        if (dstSize != this.getSize())
        {
            if (FailOnFileSize)
            {
                throw new IntegrityCheckException("Downloaded file is not the same size.", -1, sha1, expectedChecksums, dstSize, getSize(), downloadUrl, path);
            } else
            {
                LOGGER.warn("{} size incorrect.", getName());
            }
        }
    }

    public void finito() throws Exception
    {
        switch (type)
        {
            case "cf-extract":
                try {
                    FileUtils.extractFromZip(this.destination, this.path.getParent(), "overrides", true);
                    Files.delete(this.destination);
                } catch (Exception e) {
                    throw new Exception("Unable to extract overrides due to error", e);
                }
        }
    }

    public Path getPath()
    {
        return path;
    }

    public String getUrl()
    {
        return downloadUrl;
    }

    public HashCode getSha1()
    {
        return sha1;
    }

    public List<HashCode> getExpectedSha1()
    {
        return expectedChecksums;
    }

    public long getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public String getType()
    {
        return type;
    }

    public long getSize()
    {
        return size;
    }
}
