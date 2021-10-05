package net.creeperhost.creeperlauncher.util;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.nio.file.Path;

public class Artifact
{
    private String domain;
    private String name;
    private String version;
    private String classifier = null;
    private String ext = "jar";

    private String path;
    private String file;
    private String descriptor;

    public static Artifact from(String descriptor)
    {
        Artifact ret = new Artifact();
        ret.descriptor = descriptor;

        String[] pts = descriptor.split(":");
        ret.domain = pts[0];
        ret.name = pts[1];

        int last = pts.length - 1;
        int idx = pts[last].indexOf('@');
        if (idx != -1)
        {
            ret.ext = pts[last].substring(idx + 1);
            pts[last] = pts[last].substring(0, idx);
        }

        ret.version = pts[2];
        if (pts.length > 3)
            ret.classifier = pts[3];

        ret.file = ret.name + '-' + ret.version;
        if (ret.classifier != null) ret.file += '-' + ret.classifier;
        ret.file += '.' + ret.ext;

        ret.path = ret.domain.replace('.', '/') + '/' + ret.name + '/' + ret.version + '/' + ret.file;

        return ret;
    }

    public Path getLocalPath(Path base)
    {
        return base.resolve(path);
    }

    public String getDescriptor()
    {
        return descriptor;
    }

    public String getPath()
    {
        return path;
    }

    public String getDomain()
    {
        return domain;
    }

    public String getName()
    {
        return name;
    }

    public String getVersion()
    {
        return version;
    }

    public String getClassifier()
    {
        return classifier;
    }

    public String getExt()
    {
        return ext;
    }

    public String getFilename()
    {
        return file;
    }

    @Override
    public String toString()
    {
        return getDescriptor();
    }

    public static class Adapter implements JsonDeserializer<Artifact>, JsonSerializer<Artifact>
    {
        @Override
        public JsonElement serialize(Artifact src, Type typeOfSrc, JsonSerializationContext context)
        {
            return src == null ? JsonNull.INSTANCE : new JsonPrimitive(src.getDescriptor());
        }

        @Override
        public Artifact deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
        {
            return json.isJsonPrimitive() ? Artifact.from(json.getAsJsonPrimitive().getAsString()) : null;
        }
    }
}
