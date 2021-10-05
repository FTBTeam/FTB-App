package net.creeperhost.creeperlauncher.pack;

import java.nio.file.Path;
import java.util.List;

public interface IPack
{
    long getId();

    String getName();

    String getVersion();

    Path getDir();

    List<String> getAuthors();

    String getDescription();

    String getMcVersion();

    String getUrl();

    String getArtURL();

    int getMinMemory();

    int getRecMemory();
}
