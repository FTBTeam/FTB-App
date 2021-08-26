package net.creeperhost.creeperlauncher.os;

import net.creeperhost.creeperlauncher.Constants;

public class OSUtils
{

    public static String getVersion()
    {
        return System.getProperty("os.version").toLowerCase();
    }

}
