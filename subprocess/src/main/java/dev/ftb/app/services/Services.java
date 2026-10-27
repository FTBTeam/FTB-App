package dev.ftb.app.services;

import dev.ftb.app.AppFlags;
import dev.ftb.app.AppPaths;
import dev.ftb.app.Instances;
import dev.ftb.app.accounts.AccountManager;
import dev.ftb.app.api.RcpApi;
import dev.ftb.app.services.cache.AppCache;
import dev.ftb.app.storage.settings.Settings;
import net.covers1624.jdkutils.JdkInstallationManager;

public interface Services {
    AppCache cache();
    AccountManager accounts();
    AppPaths paths();
    AppFlags flags();
    Settings settings();
    RcpApi rcpApi();
    Instances instances();
    AppThreads threads();
    JdkInstallationManager jdkInstallationManager();
    AppHttp http();
}
