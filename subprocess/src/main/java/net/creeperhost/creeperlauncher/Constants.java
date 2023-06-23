package net.creeperhost.creeperlauncher;

import net.covers1624.jdkutils.AdoptiumProvisioner;
import net.covers1624.jdkutils.JdkInstallationManager;
import net.covers1624.quack.net.okhttp.MultiHasherInterceptor;
import net.covers1624.quack.net.okhttp.OkHttpDownloadAction;
import net.covers1624.quack.net.okhttp.ThrottlerInterceptor;
import net.creeperhost.creeperlauncher.os.OS;
import net.creeperhost.creeperlauncher.util.*;
import net.creeperhost.minetogether.lib.util.SignatureUtil;
import okhttp3.*;
import okio.Throttler;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
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
    public static final String MC_RESOURCES_MIRROR = "https://azuresucks.modpacks.ch/";
    public static final String MC_LIBS = "https://libraries.minecraft.net/";
    public static final String MC_LAUNCHER = "https://launcher.mojang.com/download/Minecraft.";
    public static final String MC_SESSION_SERVER_JOIN = "https://sessionserver.mojang.com/session/minecraft/join";

    //API
    public static final String CREEPERHOST_MODPACK = CreeperLauncher.isDevMode ? "https://modpack-api.ch.tools" : "https://api.modpacks.ch";
    public static final String CREEPERHOST_MODPACK_SEARCH2 = CREEPERHOST_MODPACK + "/public/modpack/";
    public static final String MOD_API = CREEPERHOST_MODPACK + "/public/mod/";
    public static final String JSON_PROXY = "https://api.modpacks.ch/public/meta/proxy/";

    public static final String CH_MAVEN = "https://maven.creeperhost.net/";
    public static final String MC_JSONS = "https://apps.modpacks.ch/versions/minecraftjsons/";

    public static final String UPLOAD_TRANSFER_HOST = "http://upload.share.modpacks.ch/";
    public static final String TRANSFER_HOST = "https://share.modpacks.ch/get/";

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

    // Microsoft Authentication Dance
    public static final String MS_OAUTH_XBL_AUTHENTICATE = "https://user.auth.xboxlive.com/user/authenticate";
    public static final String MS_OAUTH_XSTS_AUTHORIZE = "https://xsts.auth.xboxlive.com/xsts/authorize";
    public static final String MS_OAUTH_LAUNCHER_LOGIN = "https://api.minecraftservices.com/launcher/login";
    public static final String MS_OAUTH_CHECK_STORE = "https://api.minecraftservices.com/entitlements/mcstore";
    public static final String MC_GET_PROFILE = "https://api.minecraftservices.com/minecraft/profile";
    public static final String MC_CHECK_MIGRATION = "https://api.minecraftservices.com/rollout/v1/msamigration";
    
    //Other
    public static final int WEBSOCKET_PORT = 13377;
    public static final String APPVERSION = "@APPVERSION@";
    public static final String BRANCH = "@BRANCH@";
    public static final String SENTRY_DSN = "@SENTRY@";
    public static final String PLATFORM = WORKING_DIR.toAbsolutePath().toString().contains("Overwolf") ? "Overwolf" : "Electron";
    public static final String USER_AGENT = "modpacklauncher/" + APPVERSION + " Mozilla/5.0 (" + OS.CURRENT.name() + ") AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.138 Safari/537.36 Vivaldi/1.8.770.56";
    private static final Throttler GLOBAL_THROTTLER = new Throttler();

    // 50MB cache for DNS.
    private static final Cache DOH_CACHE = new Cache(getDataDir().resolve(".doh_cache").toFile(), 50 * 1024 * 1024);

    public static final DNSChain DNS_CHAIN = new DNSChain(
            new DNSChain.DnsOverHttpsStep(DOH_CACHE, DOHHost.CLOUDFLARE),
            new DNSChain.DnsOverHttpsStep(DOH_CACHE, DOHHost.GOOGLE),
            new DNSChain.DnsOverHttpsStep(DOH_CACHE, DOHHost.CREEPERHOST),
            new DNSChain.SystemDNSStep()
    );

    @Nullable
    private static OkHttpClient OK_HTTP_CLIENT;
    @Nullable
    private static OkHttpClient OK_HTTP_1_CLIENT;

    @Nullable
    private static JdkInstallationManager JDK_INSTALL_MANAGER;

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

    public static String LIB_SIGNATURE = SignatureUtil.getSignature();

    public static void refreshHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .dns(new DNSChain.OkHTTPAdapter(DNS_CHAIN))
                .connectTimeout(5, TimeUnit.MINUTES)
                .readTimeout(5, TimeUnit.MINUTES)
                .connectionPool(new ConnectionPool())
                .cookieJar(new SimpleCookieJar())
                .addInterceptor(new ThrottlerInterceptor())
                .addInterceptor(new MultiHasherInterceptor())
                .addInterceptor(chain -> chain.proceed(chain.request().newBuilder().header("User-Agent", USER_AGENT).build()));

        SSLUtils.inject(builder);
        ProxyUtils.inject(builder);
        OK_HTTP_CLIENT = builder.build();
        builder.protocols(List.of(Protocol.HTTP_1_1));
        OK_HTTP_1_CLIENT = builder.build();
    }

    public static OkHttpClient httpClient() {
        if (OK_HTTP_CLIENT == null) {
            refreshHttpClient();
        }
        return OK_HTTP_CLIENT;
    }

    public static OkHttpClient http1Client() {
        if (OK_HTTP_1_CLIENT == null) {
            refreshHttpClient();
        }
        return OK_HTTP_1_CLIENT;
    }

    public static JdkInstallationManager getJdkManager() {
        if (JDK_INSTALL_MANAGER == null) {
            JDK_INSTALL_MANAGER = new JdkInstallationManager(
                    Constants.BIN_LOCATION.resolve("runtime"),
                    new AdoptiumProvisioner(() -> new OkHttpDownloadAction()
                            .setClient(httpClient())
                            .addTag(Throttler.class, Constants.getGlobalThrottler())
                    ),
                    true
            );
        }
        return JDK_INSTALL_MANAGER;
    }

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
