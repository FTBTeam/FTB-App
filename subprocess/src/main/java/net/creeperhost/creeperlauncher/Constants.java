package net.creeperhost.creeperlauncher;

import net.covers1624.jdkutils.AdoptiumProvisioner;
import net.covers1624.jdkutils.JdkInstallationManager;
import net.covers1624.quack.net.okhttp.MultiHasherInterceptor;
import net.covers1624.quack.net.okhttp.OkHttpDownloadAction;
import net.covers1624.quack.net.okhttp.ThrottlerInterceptor;
import net.creeperhost.creeperlauncher.util.SimpleCookieJar;
import net.creeperhost.creeperlauncher.os.OS;
import net.creeperhost.minetogether.lib.util.SignatureUtil;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okio.Throttler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Constants {
    // Don't put a Logger in this class, it is called before log4j can be initialized.
    // Adding a logger will break sentry!

    //CWD
    public static final Path WORKING_DIR = Paths.get(System.getProperty("user.dir"));
    private static final String INNER_DATA_DIR = ".ftba";
    private static final Path DATA_DIR = Paths.get(System.getProperty("user.home"), INNER_DATA_DIR);

    //Launcher titles
    public static final String windowTitle = "FTBApp";

    //Mojang
    public static final String MC_VERSION_MANIFEST = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    public static final String MC_RESOURCES = "https://resources.download.minecraft.net/";
    public static final String MC_LIBS = "https://libraries.minecraft.net/";
    public static final String MC_LAUNCHER = "https://launcher.mojang.com/download/Minecraft.";

    //API
    public static final String CREEPERHOST_MODPACK = CreeperLauncher.isDevMode ? "https://modpack-api.ch.tools" : "https://api.modpacks.ch";
    public static final String CREEPERHOST_MODPACK_SEARCH2 = CREEPERHOST_MODPACK + "/public/modpack/";
    public static final String SHARE_API = CREEPERHOST_MODPACK + Constants.KEY + "/modpack/share/";
    public static final String MOD_API = CREEPERHOST_MODPACK + "/public/mod/";

    public static final String CH_MAVEN = "https://maven.creeperhost.net/";
    public static final String MC_JSONS = "https://apps.modpacks.ch/versions/minecraftjsons/";

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
    public static final Path LIBRARY_LOCATION = BIN_LOCATION.resolve("libraries");

    //Other
    public static final int WEBSOCKET_PORT = 13377;
    public static final String APPVERSION = "@APPVERSION@";
    public static final String BRANCH = "@BRANCH@";
    public static final String SENTRY_DSN = "@SENTRY@";
    public static final String PLATFORM = WORKING_DIR.toAbsolutePath().toString().contains("Overwolf") ? "Overwolf" : "Electron";
    public static final String USER_AGENT = "modpacklauncher/" + APPVERSION + " Mozilla/5.0 (" + OS.CURRENT.name() + ") AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.138 Safari/537.36 Vivaldi/1.8.770.56";
    private static final Throttler GLOBAL_THROTTLER = new Throttler();

    public static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.MINUTES)
            .readTimeout(5, TimeUnit.MINUTES)
            .connectionPool(new ConnectionPool())
            .cookieJar(new SimpleCookieJar())
            .addInterceptor(new ThrottlerInterceptor())
            .addInterceptor(new MultiHasherInterceptor())
            .addInterceptor(chain -> chain.proceed(chain.request().newBuilder().removeHeader("User-Agent").addHeader("User-Agent", USER_AGENT).build()))
            .build();

    public static JdkInstallationManager JDK_INSTALL_MANAGER = new JdkInstallationManager(
            Constants.BIN_LOCATION.resolve("runtime"),
            new AdoptiumProvisioner(() -> new OkHttpDownloadAction()
                    .setClient(OK_HTTP_CLIENT)
                    .addTag(Throttler.class, Constants.getGlobalThrottler())
            ),
            true
    );

    // Default arguments applied to all instances in the Minecraft launcher as of 29/11/2021
    public static final String[] MOJANG_DEFAULT_ARGS = {
            "-Xmx${memory}M",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+UseG1GC",
            "-XX:G1NewSizePercent=20",
            "-XX:G1ReservePercent=20",
            "-XX:MaxGCPauseMillis=50",
            "-XX:G1HeapRegionSize=32M"
    };

    public static final String LOG4JPATCHER_URL = "https://github.com/CreeperHost/Log4jPatcher/releases/download/v1.0.1/Log4jPatcher-1.0.1.jar";

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

    public static String LIB_SIGNATURE = SignatureUtil.getSignature();

    public static String getCreeperhostModpackPrefix(boolean isPrivate, byte packType) {
        String key = "public";
        String typeSlug = switch (packType) {
            case 1 -> "curseforge";
            default -> "modpack";
        };

        if (packType == 1) {
            // CurseForge has no private packs.
            isPrivate = false;
        }

        if (!Constants.KEY.isEmpty() && isPrivate) {
            key = Constants.KEY;
        }
        return Constants.CREEPERHOST_MODPACK + "/" + key + "/" + typeSlug + "/";
    }

    public static Path getDataDir() {
        if (System.getProperty("ftba.dataDirOverride") != null) {
            return Path.of(System.getProperty("ftba.dataDirOverride")).toAbsolutePath().normalize();
        }

        Path ret = switch (OS.CURRENT) {
            case WIN -> Paths.get(System.getenv("LOCALAPPDATA"), INNER_DATA_DIR);
            case MAC -> Paths.get(System.getProperty("user.home"), "Library", "Application Support", INNER_DATA_DIR);
            default -> DATA_DIR;
        };
        return ret.toAbsolutePath().normalize();
    }

    public static Throttler getGlobalThrottler() {
        // TODO, double-check this logic.
        long maxSpeed = Settings.getSpeedLimit() * 1000L;
        if (maxSpeed > 0) {
            GLOBAL_THROTTLER.bytesPerSecond((maxSpeed / 8) * 2); //Some reason it always limits to 50% for me, so we multiply!
        } else {
            GLOBAL_THROTTLER.bytesPerSecond(Long.MAX_VALUE);
        }
        return GLOBAL_THROTTLER;
    }
}
