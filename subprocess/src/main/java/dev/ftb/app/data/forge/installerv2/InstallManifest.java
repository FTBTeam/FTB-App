package dev.ftb.app.data.forge.installerv2;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.covers1624.quack.gson.MavenNotationAdapter;
import net.covers1624.quack.maven.MavenNotation;
import dev.ftb.app.minecraft.jsons.VersionManifest;

import java.util.*;

/**
 * Created by covers1624 on 17/1/22.
 */
public class InstallManifest {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(MavenNotation.class, new MavenNotationAdapter())
            .create();

    public int spec = 0;

    // V0
    public String profile;
    public String version;
    public String minecraft;
    public String json;
    public MavenNotation path;
    public List<VersionManifest.Library> libraries = new LinkedList<>();
    public Map<String, DataEntry> data = new HashMap<>();
    public List<Processor> processors = new LinkedList<>();

    // V1
    public String serverJarPath = "{ROOT}/minecraft_server.{MINECRAFT_VERSION}.jar";

    public static class DataEntry {

        public String client;
        public String server;
    }

    public static class Processor {
        public List<String> sides = new LinkedList<>();
        public MavenNotation jar;
        public List<MavenNotation> classpath = new LinkedList<>();
        public List<String> args = new LinkedList<>();
        public Map<String, String> outputs = new HashMap<>();
    }
}
