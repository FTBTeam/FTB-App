package dev.ftb.app.pack;

import dev.ftb.app.AppPaths;
import dev.ftb.app.Instances;
import dev.ftb.app.accounts.AccountManager;
import dev.ftb.app.api.RcpApi;
import dev.ftb.app.data.modpack.ModpackManifest;
import dev.ftb.app.data.modpack.ModpackVersionManifest;
import dev.ftb.app.services.AppHttp;
import dev.ftb.app.services.AppThreads;
import dev.ftb.app.services.Services;
import dev.ftb.app.services.cache.AppCache;
import dev.ftb.app.storage.settings.Settings;
import net.covers1624.jdkutils.JdkInstallationManager;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

public record InstanceFactory(Settings settings, AppPaths appPaths, AppThreads threads, AppCache cache,
                              AppHttp httpClient, AccountManager accounts, Instances instances, RcpApi rcpApi,
                              JdkInstallationManager jdkInstallationManager) {

    /// Creates a new instance based on the provided modpack and version manifest. The name, artPath, and categoryId can be null, in which case defaults will be used.
    public Instance create(@Nullable String name, @Nullable String artPath, @Nullable UUID categoryId, ModpackManifest modpack, ModpackVersionManifest versionManifest, String mcVersion, boolean isPrivate, byte packType) {
        return new Instance(
            this, name, artPath, categoryId, modpack, versionManifest, mcVersion, isPrivate, packType
        );
    }

    public Instance load(Path path, Path json) throws IOException {
        return new Instance(this, path, json);
    }
    
    // This is cheating but it's nice for the websocket handler.
    public static InstanceFactory fromServices(Services services) {
        return new InstanceFactory(
            services.settings(),
            services.paths(),
            services.threads(),
            services.cache(),
            services.http(),
            services.accounts(),
            services.instances(),
            services.rcpApi(),
            services.jdkInstallationManager()
        );
    }
}
