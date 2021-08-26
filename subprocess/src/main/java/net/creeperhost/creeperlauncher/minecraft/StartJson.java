package net.creeperhost.creeperlauncher.minecraft;

public class StartJson
{
    String id;
    String time;
    String releaseType;
    String minecraftArguments;
    String mainClass;
    String inheritsFrom;
    String jar;
    Libraries[] libraries;

    public StartJson(String id, String time, String releaseType, String minecraftArguments, String mainClass, String inheritsFrom, String jar, Libraries[] libraries)
    {
        this.id = id;
        this.time = time;
        this.releaseType = releaseType;
        this.minecraftArguments = minecraftArguments;
        this.mainClass = mainClass;
        this.inheritsFrom = inheritsFrom;
        this.jar = jar;
        this.libraries = libraries;
    }

    public String getId()
    {
        return id;
    }

    public String getTime()
    {
        return time;
    }

    public String getReleaseType()
    {
        return releaseType;
    }

    public String getMinecraftArguments()
    {
        return minecraftArguments;
    }

    public String getMainClass()
    {
        return mainClass;
    }

    public String getInheritsFrom()
    {
        return inheritsFrom;
    }

    public String getJar()
    {
        return jar;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public void setJar(String jar)
    {
        this.jar = jar;
    }

    public void setInheritsFrom(String inheritsFrom)
    {
        this.inheritsFrom = inheritsFrom;
    }

    public void setMainClass(String mainClass)
    {
        this.mainClass = mainClass;
    }

    public void setMinecraftArguments(String minecraftArguments)
    {
        this.minecraftArguments = minecraftArguments;
    }

    public void setReleaseType(String releaseType)
    {
        this.releaseType = releaseType;
    }

    public void setTime(String time)
    {
        this.time = time;
    }

    public void setLibraries(Libraries[] libraries)
    {
        this.libraries = libraries;
    }

    public Libraries[] getLibraries()
    {
        return libraries;
    }

    public static class Libraries
    {
        String name;
        String url;
        boolean serverreq;
        boolean clientreq;
        Natives natives;
        Rules[] rule;

        public Libraries(String name, String url, boolean serverreq, boolean clientreq, Natives natives, Rules[] rule)
        {
            this.name = name;
            this.url = url;
            this.serverreq = serverreq;
            this.clientreq = clientreq;
            this.natives = natives;
            this.rule = rule;
        }

        public String getName()
        {
            return name;
        }

        public String getUrl()
        {
            return url;
        }

        public boolean isClientReq()
        {
            return clientreq;
        }

        public boolean isServerReq()
        {
            return serverreq;
        }

        public Natives getNatives()
        {
            return natives;
        }

        public Rules[] getRule()
        {
            return rule;
        }
    }

    public class Rules
    {
        String action;
        Os os;

        public Rules(String action, Os os1)
        {
            this.action = action;
            this.os = os1;
        }

        public Os getOs()
        {
            return os;
        }

        public String getAction()
        {
            return action;
        }
    }

    public class Os
    {
        String name;

        public Os(String name)
        {
            this.name = name;
        }

        public String getName()
        {
            return name;
        }
    }

    public class Natives
    {
        String linux;
        String windows;
        String osx;

        public Natives(String linux, String windows, String osx)
        {
            this.linux = linux;
            this.windows = windows;
            this.osx = osx;
        }


        public String getLinux()
        {
            return linux;
        }

        public String getOsx()
        {
            return osx;
        }

        public String getWindows()
        {
            return windows;
        }
    }
}
