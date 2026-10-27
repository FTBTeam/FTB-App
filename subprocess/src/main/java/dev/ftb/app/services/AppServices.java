package dev.ftb.app.services;

import com.google.common.collect.ImmutableMap;
import dev.ftb.app.AppFlags;
import dev.ftb.app.AppPaths;
import dev.ftb.app.Instances;
import dev.ftb.app.accounts.AccountManager;
import dev.ftb.app.api.RcpApi;
import dev.ftb.app.services.cache.AppCache;
import dev.ftb.app.storage.settings.Settings;
import dev.ftb.app.util.ProxyUtils;
import net.covers1624.jdkutils.JdkInstallationManager;
import net.covers1624.jdkutils.provisioning.adoptium.AdoptiumProvisioner;
import net.covers1624.quack.net.httpapi.okhttp.OkHttpEngine;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
    private final AppHttp appHttp;
    private final AppStorage appStorage;
    
    public AppServices(ImmutableMap<String, String> args) {
        this.flags = new AppFlags(
            args.containsKey("dev"),
            args.containsKey("overwolf") ? AppFlags.FrontendPlatform.OVERWOLF : AppFlags.FrontendPlatform.ELECTRON
        );

        this.paths = new AppPaths();
        this.settings = new Settings(this.paths);
        this.threads = new AppThreads(this.settings.getThreadLimit());
        this.accounts = new AccountManager(this.paths);
        
        this.appHttp = new AppHttp(this.flags, this.settings);
        this.jdkInstallationManager = this.createJdkManager(this.appHttp);
        this.appStorage = new AppStorage(this.paths);

        this.cache = new AppCache(this.paths, this.settings, this.threads, this.http());
        
        this.settings.onChange((newSettings) -> {
            this.cache.local().updateLocationIfNeeded();
            // If something changed, recreate the http clients.
            ProxyUtils.loadProxy(this.appStorage, this.settings, this.appHttp::createClients);
        });
        
        this.rcpApi = new RcpApi(this);
        this.instances = new Instances(this.paths, this.settings, this.rcpApi, this.accounts, this.threads, this.cache, this.appHttp, this.jdkInstallationManager);
    }
    
    /**
     * Returns the JDK installation manager, if something goes wrong during initialization, we'll purge its index of installed
     * JDKs and try again.
     *
     * This is likely not ideal, but it also should rarely happen so it's probably fine.
     */
    public JdkInstallationManager createJdkManager(AppHttp appHttp) {
        Path runtimeDir = this.paths.binDir().resolve("runtime").toAbsolutePath();
        try {
            return new JdkInstallationManager(runtimeDir, jdkProvisioner(appHttp));
        } catch (Throwable e) {
            LOGGER.error("Failed to initialise JdkInstallationManager, purging cached installs and retrying: {}", String.valueOf(e));
            try {
                Files.deleteIfExists(runtimeDir.resolve("installations.json"));
            } catch (IOException ignored) {
            }
            
            return new JdkInstallationManager(runtimeDir, jdkProvisioner(appHttp));
        }
    }

    private AdoptiumProvisioner jdkProvisioner(AppHttp http) {
        return new AdoptiumProvisioner(new OkHttpEngine() {
            @Override
            protected OkHttpClient getClient() {
                return http.httpClient();
            }
        });
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
        return this.jdkInstallationManager;
    }

    @Override
    public AppHttp http() {
        return this.appHttp;
    }

    @Override
    public AppStorage storage() {
        return this.appStorage;
    }

    @Override
    public AppCache cache() {
        return this.cache;
    }
    
    
}
