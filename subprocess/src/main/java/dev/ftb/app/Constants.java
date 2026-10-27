package dev.ftb.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class Constants {
    private static final Logger LOGGER = LoggerFactory.getLogger(Constants.class);
    
    public static final String CF_API_KEY = BuildInfo.CF_API_KEY;
    public static final String APPVERSION = BuildInfo.APPVERSION;
    public static final String BRANCH = BuildInfo.BRANCH;
    public static final String COMMIT = BuildInfo.COMMIT;
    
    //Mojang
    public static final String MC_VERSIONS = "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json";
    public static final String MC_RESOURCES = "https://resources.download.minecraft.net/";
    public static final String MC_SESSION_SERVER_JOIN = "https://sessionserver.mojang.com/session/minecraft/join";

    //API
    public static final String FTB_MODPACKS_API = "https://api.feed-the-beast.com/v1/modpacks";
    public static final String META_JSON = FTB_MODPACKS_API + "/public/helpers/meta";
    public static final String MC_JSONS = FTB_MODPACKS_API + "/public/helpers/versions/minecraftjsons/";
    
    //Other
    public static final int WEBSOCKET_PORT = 13377;
    
    public static final String WEBSOCKET_SECRET = UUID.randomUUID().toString();
    
    public static final String[] UNDER_26_ARGS = new String[] {
        "-XX:+UnlockExperimentalVMOptions",
        "-XX:+UseG1GC",
        "-XX:G1NewSizePercent=20",
        "-XX:G1ReservePercent=20",
        "-XX:MaxGCPauseMillis=50",
        "-XX:G1HeapRegionSize=32M"
    };

    public static final String[] OVER_26_ARGS = new String[]{
        "-XX:+UseCompactObjectHeaders",
        "-XX:+AlwaysPreTouch",
        "-XX:+UseStringDeduplication",
        "-XX:+UseZGC"
    };
    
    public static String[] jvmArsFromMcVersion(String mcVersion) {
        if (mcVersion.startsWith("1.")) {
            return Constants.UNDER_26_ARGS;
        } else {
            return Constants.OVER_26_ARGS;
        }
    }
    
    public static String curseForgeApiKey() {
        if (CF_API_KEY.startsWith("@") && CF_API_KEY.endsWith("@")) {
            String env = System.getenv("CF_API_KEY");
            return env != null ? env : "";
        }
        
        return CF_API_KEY;
    }
}
