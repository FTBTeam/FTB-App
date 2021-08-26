package net.creeperhost.creeperlauncher;

import net.creeperhost.creeperlauncher.os.OS;
import net.creeperhost.creeperlauncher.os.OSUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Constants
{
    private static final Logger LOGGER = LogManager.getLogger();

    //CWD
    public static final Path WORKING_DIR = Paths.get(System.getProperty("user.dir"));
    private static final String INNER_DATA_DIR = ".ftba";
    private static final Path DATA_DIR = Paths.get(System.getProperty("user.home"), INNER_DATA_DIR);

    //Launcher titles
    public static final String windowTitle = "FTBApp";

    //Mojang
    public static final String MC_VERSION_MANIFEST = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    public static final String MC_RESOURCES = "http://resources.download.minecraft.net/";
    public static final String MC_LIBS = "https://libraries.minecraft.net/";
    public static final String MC_LAUNCHER = "https://launcher.mojang.com/download/Minecraft.";

    //API
    public static final String CREEPERHOST_MODPACK = CreeperLauncher.isDevMode ? "https://modpack-api.ch.tools" : "https://api.modpacks.ch";
    public static final String CREEPERHOST_MODPACK_SEARCH2 = CREEPERHOST_MODPACK + "/public/modpack/";
    public static final String SHARE_API = CREEPERHOST_MODPACK + Constants.KEY + "/modpack/share/";
    public static final String MOD_API = CREEPERHOST_MODPACK + "/public/mod/";

    //Forge
    public static final String FORGE_XML = "https://files.minecraftforge.net/maven/net/minecraftforge/forge/maven-metadata.xml";
    public static final String FORGE_MAVEN = "https://files.minecraftforge.net/maven/net/minecraftforge/forge/";
    public static final String FORGE_RECOMMENDED = "https://files.minecraftforge.net/maven/net/minecraftforge/forge/promotions_slim.json";
    public static final String FORGE_CH = "https://maven.creeperhost.net/net/minecraftforge/forge/";

    //Paths
    public static final Path BIN_LOCATION_OURS = WORKING_DIR.resolve("bin");
    public static final Path BIN_LOCATION = getDataDir().resolve("bin");

    public static final Path VERSIONS_FOLDER_LOC = getDataDir().resolve(Paths.get("bin", "versions"));
    public static final Path INSTANCES_FOLDER_LOC = getDataDir().resolve("instances");
    public static final String LAUNCHER_PROFILES_JSON_NAME = "launcher_profiles.json";
    public static final Path LAUNCHER_PROFILES_JSON = BIN_LOCATION.resolve(LAUNCHER_PROFILES_JSON_NAME);
    public static final Path LIBRARY_LOCATION = BIN_LOCATION.resolve("libraries");
    public static final Path OLD_CACHE_LOCATION = getDataDir().resolve(".localCache");

    //Other
    public static final int WEBSOCKET_PORT = 13377;
    public static final String APPVERSION = "@APPVERSION@";
    public static final String BRANCH = "@BRANCH@";
    public static final String PLATFORM = WORKING_DIR.toAbsolutePath().toString().contains("Overwolf") ? "Overwolf" : "Electron";

    //Auth
    public static String KEY = "";
    public static String SECRET = "";

    //MT Identifiers
    public static String MT_HASH = "";
    public static Path MTCONNECT_DIR = getDataDir().resolve("MTConnect");

    //S3 Auth
    public static String S3_KEY = "";
    public static String S3_SECRET = "";
    public static String S3_BUCKET = "";
    public static String S3_HOST = "";


    public static String getCreeperhostModpackSearch2(boolean _private, byte packType)
    {
        String typeSlug = "modpack";

        switch (packType)
        {
            case 1:
                return Constants.CREEPERHOST_MODPACK + "/public/" + "curseforge" + "/"; // no such thing as private curse modpacks, rest of logic not needed
        }
        if(Constants.KEY.isEmpty() || !_private)
        {
            return Constants.CREEPERHOST_MODPACK + "/public/" + typeSlug + "/";
        }
        if(Constants.KEY.isEmpty() && _private)
        {
            LOGGER.error("Tried to access a private pack without having configured the secret and key.");
        }
        return Constants.CREEPERHOST_MODPACK + "/" + Constants.KEY + "/" + typeSlug + "/";
    }
    public static Path getDataDir()
    {
        Path ret = DATA_DIR;
        switch (OS.CURRENT) {
            case WIN:
                ret = Paths.get(System.getenv("LOCALAPPDATA"), INNER_DATA_DIR);
                break;
            case MAC:
                ret = Paths.get(System.getProperty("user.home"), "Library", "Application Support", INNER_DATA_DIR);
                break;
        }
        return ret.toAbsolutePath().normalize();
    }

    public static Path getDataDirOld() {
        return DATA_DIR.toAbsolutePath();
    }
}
