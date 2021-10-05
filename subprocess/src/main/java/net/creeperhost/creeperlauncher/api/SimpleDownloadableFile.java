package net.creeperhost.creeperlauncher.api;

import java.nio.file.Path;

public class SimpleDownloadableFile
{
    String version;
    Path path;
    long size;
    boolean clientSide;
    boolean optional;
    long id;
    String name;
    String type;
    String sha1;

    public SimpleDownloadableFile(String version, Path path, long size, boolean clientSide, boolean optional, long id, String name, String type)
    {
        this.version = version;
        this.path = path;
        this.size = size;
        this.clientSide = clientSide;
        this.optional = optional;
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public String getVersion()
    {
        return version;
    }

    public Path getPath()
    {
        return path;
    }

    public String getSha1()
    {
        return sha1;
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
