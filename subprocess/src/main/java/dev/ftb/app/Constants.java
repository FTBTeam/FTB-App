package dev.ftb.app;

import dev.ftb.app.os.OS;
import dev.ftb.app.storage.settings.Settings;
import dev.ftb.app.util.FetchLoggerInterceptor;
import dev.ftb.app.util.ProxyUtils;
import dev.ftb.app.util.SSLUtils;
import dev.ftb.app.util.SimpleCookieJar;
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

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Constants {
    // Don't put a Logger in this class, it is called before log4j can be initialized.
    // Adding a logger will break sentry!

    public static boolean IS_DEV_MODE = false;
    public static String PLATFORM = "unset";
    
    //Mojang
    public static final String MC_VERSIONS = "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json";
    public static final String MC_RESOURCES = "https://resources.download.minecraft.net/";
    public static final String MC_SESSION_SERVER_JOIN = "https://sessionserver.mojang.com/session/minecraft/join";

    //API
    public static final String FTB_MODPACKS_API = "https://api.feed-the-beast.com/v1/modpacks";
    public static final String META_JSON = FTB_MODPACKS_API + "/public/helpers/meta";

    public static final String CH_MAVEN = "https://maven.creeperhost.net/";
    public static final String MC_JSONS = FTB_MODPACKS_API + "/public/helpers/versions/minecraftjsons/";
    
    //Other
    public static final int WEBSOCKET_PORT = 13377;
    public static final String APPVERSION = "@APPVERSION@";
    public static final String BRANCH = "@BRANCH@";
    public static final String COMMIT = "@COMMIT@";
    
    public static final String USER_AGENT = "ftb-app/" + APPVERSION + " OS/"+ OS.CURRENT.name() + " Platform/" + PLATFORM + " Branch/" + BRANCH + " Commit/" + COMMIT;
    private static final Throttler GLOBAL_THROTTLER = new Throttler();

    public static final String WEBSOCKET_SECRET = UUID.randomUUID().toString();

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
                    AppMain.paths().binDir().resolve("runtime"),
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
