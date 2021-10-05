package net.creeperhost.creeperlauncher.install.tasks.http;

import com.google.common.hash.HashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

@SuppressWarnings ("UnstableApiUsage")
public class DownloadedFile
{
    @NotNull
    private final Path destination;
    private final long size;
    @Nullable
    private final HashCode checksum;

    public DownloadedFile(Path destination, long size, HashCode checksum)
    {
        this.destination = destination;
        this.size = size;
        this.checksum = checksum;
    }

    public Path getDestination()
    {
        return destination;
    }

    public long getSize()
    {
        return size;
    }

    public HashCode getChecksum()
    {
        return checksum;
    }
}
