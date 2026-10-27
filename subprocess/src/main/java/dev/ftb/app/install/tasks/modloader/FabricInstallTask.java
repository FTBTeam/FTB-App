package dev.ftb.app.install.tasks.modloader;

import dev.ftb.app.AppPaths;
import dev.ftb.app.install.tasks.DownloadTask;
import dev.ftb.app.install.tasks.TaskProgressListener;
import dev.ftb.app.minecraft.jsons.VersionManifest;
import dev.ftb.app.pack.CancellationToken;
import dev.ftb.app.services.AppHttp;
import net.covers1624.quack.gson.JsonUtils;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;

public abstract class FabricInstallTask extends ModLoaderInstallTask {

    private static final String FABRIC_META = "https://meta.fabricmc.net/";

    private final String mcVersion;
    private final String loaderVersion;

    private final String versionName;
    private final AppPaths paths;
    private final AppHttp http;

    private FabricInstallTask(AppPaths paths, AppHttp http, String loaderName, String mcVersion, String loaderVersion) {
        this.paths = paths;
        this.http = http;
        this.mcVersion = mcVersion;
        this.loaderVersion = loaderVersion;

        versionName = loaderName + "-" + mcVersion + "-" + loaderVersion;
    }

    public static FabricInstallTask fabric(AppPaths paths, AppHttp http, String mcVersion, String loaderVersion) {
        return new FabricInstallTask(paths, http, "fabric-loader", mcVersion, loaderVersion) {
            @Override
            protected DownloadTask getProfileDownload(Path dest) {
                return DownloadTask.builder(http)
                        .url(FABRIC_META + "v2/versions/loader/" + mcVersion + "/" + loaderVersion + "/profile/json")
                        .dest(dest)
                        .build();
            }
        };
    }

    @Override
    public void execute(@Nullable CancellationToken cancelToken, @Nullable TaskProgressListener listener) throws Throwable {
        Path versionsDir = this.paths.mcVersionsDir();
        Path librariesDir = this.paths.mcLibrariesDir();

        Path versionJson = versionsDir.resolve(versionName).resolve(versionName + ".json");

        VersionManifest loaderManifest = downloadLoaderManifest(versionJson, cancelToken);
        downloadVanilla(this.http, versionsDir, mcVersion);

        for (VersionManifest.Library library : loaderManifest.libraries) {
            if (cancelToken != null) cancelToken.throwIfCancelled();
            processLibrary(cancelToken, librariesDir, library);
        }
    }

    @Override
    public String getModLoaderTarget() {
        return versionName;
    }

    private void processLibrary(@Nullable CancellationToken token, Path librariesDir, VersionManifest.Library library) throws IOException {
        DownloadTask task = library.createDownloadTask(this.http, librariesDir, true);
        if (task == null) throw new IOException("Unable to download or locate library: " + library.name);

        if (!task.isRedundant()) {
            task.execute(token, null);
        }
    }

    protected abstract DownloadTask getProfileDownload(Path dest);

    private VersionManifest downloadLoaderManifest(Path dest, @Nullable CancellationToken cancellationToken) throws IOException {
        DownloadTask task = getProfileDownload(dest);
        if (!task.isRedundant()) {
            task.execute(cancellationToken, null);
        }

        return JsonUtils.parse(VersionManifest.GSON, dest, VersionManifest.class);
    }
}
