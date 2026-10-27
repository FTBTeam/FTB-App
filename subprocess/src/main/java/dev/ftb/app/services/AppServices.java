package dev.ftb.app.services;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import dev.ftb.app.*;
import dev.ftb.app.accounts.AccountManager;
import dev.ftb.app.api.RcpApi;
import dev.ftb.app.os.OS;
import dev.ftb.app.services.cache.AppCache;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class AppServices implements Services {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppServices.class);
    
    private final AppFlags flags;
    private final AppPaths paths;

    private final RcpApi rcpApi;
    private final AccountManager accounts;
    private final Settings settings;
    private final Instances instances;
    private final AppCache cache;
    private final AppThreads threads;
    private final JdkInstallationManager jdkInstallationManager;
    
    // Not publicly exposed in any way.
    private final Throttler clientThrottler = new Throttler();
    private final Supplier<String> userAgent;
    
    private OkHttpClient httpClientV2;
    private OkHttpClient httpClientV1;
    
    public AppServices(ImmutableMap<String, String> args) {
        this.flags = new AppFlags(
            args.containsKey("dev"),
            args.containsKey("overwolf") ? AppFlags.FrontendPlatform.OVERWOLF : AppFlags.FrontendPlatform.ELECTRON
        );

        this.paths = new AppPaths();
        this.settings = new Settings(this.paths);
        this.threads = new AppThreads(this.settings.getThreadLimit());
        this.accounts = new AccountManager(this.paths);
        
        // This needs to happen before the client is created.
        this.userAgent = Suppliers.memoize(() -> "ftb-app/" + Constants.APPVERSION + " " +
            "OS/" + OS.CURRENT.name() + " " +
            "Platform/" + this.flags.platform() + " " +
            "Branch/" + Constants.BRANCH + " " +
            "Commit/" + Constants.COMMIT);
        
        this.jdkInstallationManager = this.createJdkManager();
        this.createHttpClient();

        this.cache = new AppCache(this.paths, this.settings, this.threads, this.httpClient());
        
        this.settings.onChange((newSettings) -> {
            this.cache.local().updateLocationIfNeeded();
            // If something changed, recreate the http clients.
            ProxyUtils.loadProxy(this.settings, this::createHttpClient);
        });
        
        this.rcpApi = new RcpApi(this);
        this.instances = new Instances(this.paths, this.settings, this.rcpApi, this.accounts, this.threads, this.cache);
    }

    public void createHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.MINUTES)
            .readTimeout(5, TimeUnit.MINUTES)
            .connectionPool(new ConnectionPool())
            .cookieJar(new SimpleCookieJar())
            .addInterceptor(new FetchLoggerInterceptor())
            .addInterceptor(new MultiHasherInterceptor())
            .addInterceptor(chain -> chain.proceed(chain.request().newBuilder().tag(Throttler.class, getClientThrottler()).build()))
            .addInterceptor(new ThrottlerInterceptor())
            .addInterceptor(chain -> chain.proceed(chain.request().newBuilder().header("User-Agent", this.userAgent.get()).build()));

        SSLUtils.inject(builder);
        ProxyUtils.inject(builder);
        this.httpClientV2 = builder.build();
        builder.protocols(List.of(Protocol.HTTP_1_1));
        this.httpClientV1 = builder.build();
    }

    /**
     * Returns the JDK installation manager, if something goes wrong during initialization, we'll purge its index of installed
     * JDKs and try again.
     *
     * This is likely not ideal, but it also should rarely happen so it's probably fine.
     */
    public JdkInstallationManager createJdkManager() {
        Path runtimeDir = this.paths.binDir().resolve("runtime").toAbsolutePath();
        try {
            return new JdkInstallationManager(runtimeDir, jdkProvisioner());
        } catch (Throwable e) {
            LOGGER.error("Failed to initialise JdkInstallationManager, purging cached installs and retrying: {}", String.valueOf(e));
            try {
                Files.deleteIfExists(runtimeDir.resolve("installations.json"));
            } catch (IOException ignored) {
            }
            
            return new JdkInstallationManager(runtimeDir, jdkProvisioner());
        }
    }

    private AdoptiumProvisioner jdkProvisioner() {
        return new AdoptiumProvisioner(new OkHttpEngine() {
            @Override
            protected OkHttpClient getClient() {
                return httpClient();
            }
        });
    }

    private Throttler getClientThrottler() {
        // TODO, double-check this logic.
        long maxSpeed = this.settings.getSpeedLimit() * 1000L;
        if (maxSpeed > 0) {
            this.clientThrottler.bytesPerSecond((maxSpeed / 8) * 2); //Some reason it always limits to 50% for me, so we multiply!
        } else {
            this.clientThrottler.bytesPerSecond(Long.MAX_VALUE);
        }
        
        return this.clientThrottler;
    }
    
    @Override
    public AccountManager accounts() {
        return this.accounts;
    }

    @Override
    public AppPaths paths() {
        return this.paths;
    }

    @Override
    public AppFlags flags() {
        return this.flags;
    }

    @Override
    public Settings settings() {
        return this.settings;
    }
    
    @Override
    public RcpApi rcpApi() {
        return this.rcpApi;
    }

    @Override
    public Instances instances() {
        return this.instances;
    }

    @Override
    public AppThreads threads() {
        return this.threads;
    }

    @Override
    public JdkInstallationManager jdkInstallationManager() {
        return null;
    }

    @Override
    public OkHttpClient httpClient() {
        return this.httpClient(Protocol.HTTP_2);
    }

    @Override
    public OkHttpClient httpClient(Protocol protocol) {
        return protocol == Protocol.HTTP_1_0 || protocol == Protocol.HTTP_1_1
            ? this.httpClientV1
            : this.httpClientV2;
    }

    @Override
    public AppCache cache() {
        return this.cache;
    }
}
