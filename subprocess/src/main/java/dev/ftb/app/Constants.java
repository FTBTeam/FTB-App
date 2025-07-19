package dev.ftb.app;

import dev.ftb.app.os.OS;
import dev.ftb.app.storage.settings.Settings;
import dev.ftb.app.util.*;
import net.covers1624.jdkutils.JdkInstallationManager;
import net.covers1624.jdkutils.provisioning.adoptium.AdoptiumProvisioner;
import net.covers1624.quack.net.httpapi.okhttp.OkHttpEngine;
import net.covers1624.quack.net.okhttp.MultiHasherInterceptor;
import net.covers1624.quack.net.okhttp.ThrottlerInterceptor;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okio.Throttler;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Constants {
    // Don't put a Logger in this class, it is called before log4j can be initialized.
    // Adding a logger will break sentry!

    public static boolean IS_DEV_MODE = false;

    //CWD
    // TODO: On linux, move to XDG_DATA_HOME 
    // TODO: On macos, don't use a hidden folder
    // TODO: On windows don't use a hidden folder
    public static final Path WORKING_DIR = Paths.get(System.getProperty("user.dir"));
    private static final String INNER_DATA_DIR = ".ftba";
    private static final Path DATA_DIR = Paths.get(System.getProperty("user.home"), INNER_DATA_DIR);
    
    //Mojang
    public static final String MC_VERSIONS = "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json";
    public static final String MC_RESOURCES = "https://resources.download.minecraft.net/";
    public static final String MC_SESSION_SERVER_JOIN = "https://sessionserver.mojang.com/session/minecraft/join";

    //API
    public static final String FTB_MODPACKS_API = "https://api.feed-the-beast.com/v1/modpacks";
    public static final String META_JSON = FTB_MODPACKS_API + "/public/helpers/meta";

    public static final String CH_MAVEN = "https://maven.creeperhost.net/";
    public static final String MC_JSONS = FTB_MODPACKS_API + "/public/helpers/versions/minecraftjsons/";

    //Paths
    public static final Path BIN_LOCATION_OURS = WORKING_DIR.resolve("bin");
    public static final Path BIN_LOCATION = getDataDir().resolve("bin");
    
    public static final Path STORAGE_DIR = getDataDir().resolve("storage");

    /**
     * @deprecated Use {@link #SETTINGS_FILE} instead.
     */
    @Deprecated
    public static final Path SETTINGS_FILE_LEGACY = BIN_LOCATION.resolve("settings.json");
    public static final Path SETTINGS_FILE = STORAGE_DIR.resolve("settings.json");
    public static final Path KV_STORE_FILE = STORAGE_DIR.resolve("storage.json");
    public static final Path CREDENTIALS_FILE = STORAGE_DIR.resolve("credentials.encr");
    
    public static final Path VERSIONS_FOLDER_LOC = getDataDir().resolve(Paths.get("bin", "versions"));
    public static final Path INSTANCES_FOLDER_LOC = getDataDir().resolve("instances");
    public static final Path LIBRARY_LOCATION = BIN_LOCATION.resolve("libraries");
    
    //Other
    public static final int WEBSOCKET_PORT = 13377;
    public static final String APPVERSION = "@APPVERSION@";
    public static final String BRANCH = "@BRANCH@";
    public static final String COMMIT = "@COMMIT@";
    public static final String PLATFORM = WORKING_DIR.toAbsolutePath().toString().contains("Overwolf") ? "Overwolf" : "Electron";
    
    public static final String USER_AGENT = "ftb-app/" + APPVERSION + " OS/"+ OS.CURRENT.name() + " Platform/" + PLATFORM + " Branch/" + BRANCH + " Commit/" + COMMIT;
    private static final Throttler GLOBAL_THROTTLER = new Throttler();

    public static final String WEBSOCKET_SECRET = UUID.randomUUID().toString();
    
    public static final CurseMetadataCache CURSE_METADATA_CACHE = new CurseMetadataCache(getDataDir().resolve(".curse_meta.json"));
    public static final ModVersionCache MOD_VERSION_CACHE = new ModVersionCache(getDataDir().resolve(".mod_meta.json"));

    @Nullable
    private static OkHttpClient OK_HTTP_CLIENT;
    @Nullable
    private static OkHttpClient OK_HTTP_1_CLIENT;

    @Nullable
    private static JdkInstallationManager JDK_INSTALL_MANAGER;

    // Default arguments applied to all instances in the Minecraft launcher as of 29/11/2021
    public static final String[] MOJANG_DEFAULT_ARGS = {
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+UseG1GC",
            "-XX:G1NewSizePercent=20",
            "-XX:G1ReservePercent=20",
            "-XX:MaxGCPauseMillis=50",
            "-XX:G1HeapRegionSize=32M"
    };

    public static void refreshHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.MINUTES)
                .readTimeout(5, TimeUnit.MINUTES)
                .connectionPool(new ConnectionPool())
                .cookieJar(new SimpleCookieJar())
                .addInterceptor(new FetchLoggerInterceptor())
                .addInterceptor(new MultiHasherInterceptor())
                .addInterceptor(chain -> chain.proceed(chain.request().newBuilder().tag(Throttler.class, getGlobalThrottler()).build()))
                .addInterceptor(new ThrottlerInterceptor())
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
                    new AdoptiumProvisioner(new OkHttpEngine() {
                        @Override
                        protected OkHttpClient getClient() {
                            return httpClient();
                        }
                    })
            );
        }
        return JDK_INSTALL_MANAGER;
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

    private static Throttler getGlobalThrottler() {
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
