package net.creeperhost.creeperlauncher.util;

public class LoaderTarget
{
    String name;
    long id;
    String type;
    String version;

    public LoaderTarget(String name, String version, long id, String type)
    {
        this.name = name;
        this.version = version;
        this.id = id;
        this.type = type;
    }

    public String getType()
    {
        return type;
    }

    public String getName()
    {
        return name;
    }

    public long getId()
    {
        return id;
    }

    public String getVersion()
    {
        return version;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this) {
            return true;
        }

        if (obj instanceof LoaderTarget) {
            LoaderTarget otherTarget = (LoaderTarget) obj;
            return  otherTarget.version.equals(this.version) &&
                    otherTarget.name.equals(this.name) &&
                    otherTarget.type.equals(this.type);
        }

        return false;
    }

    public boolean equalsNoVersion(LoaderTarget otherTarget) {
            return  otherTarget.name.equals(this.name) &&
                    otherTarget.type.equals(this.type);
    }
}
