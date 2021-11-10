package net.creeperhost.creeperlauncher.minecraft.jsons;

/**
 * Minecraft 'Versions List' manifest.
 * <p>
 * Created by covers1624 on 8/11/21.
 */
public class VersionListManifest {

    public static final String URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json";

    public static class Latest {

        public String release;
        public String snapshot;
    }

    public static class Version {

        public String id;
        public String type;
        public String url;
        public String time;
        public String releaseTime;
    }

}
