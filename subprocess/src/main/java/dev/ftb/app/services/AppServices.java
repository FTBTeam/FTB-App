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

public class AppServices implements Services {
    private final AppFlags flags;
    private final AppPaths paths;

    private final RcpApi rcpApi;
    private final AccountManager accounts;
    private final Settings settings;
    private final Instances instances;
    private final AppCache cache;
    private final AppThreads threads;
    
    public AppServices(ImmutableMap<String, String> args) {
        this.flags = new AppFlags(
            args.containsKey("dev"),
            args.containsKey("overwolf") ? AppFlags.FrontendPlatform.OVERWOLF : AppFlags.FrontendPlatform.ELECTRON
        );

        this.paths = new AppPaths();
        this.settings = new Settings(this.paths);
        this.threads = new AppThreads(this.settings.getThreadLimit());
        
        this.accounts = new AccountManager(this.paths);
        this.cache = new AppCache(this.paths, this.settings, this.threads);
        this.settings.onChange((newSettings) -> {
            this.cache.local().updateLocationIfNeeded();
            ProxyUtils.loadProxy(this.settings);
        });
        
        this.instances = new Instances(this);
        this.rcpApi = new RcpApi(this);
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
    public AppCache cache() {
        return this.cache;
    }
}
