package dev.ftb.app.install.tasks.modloader;

import net.covers1624.quack.gson.JsonUtils;
import dev.ftb.app.Constants;
import dev.ftb.app.install.tasks.DownloadTask;
import dev.ftb.app.install.tasks.TaskProgressListener;
import dev.ftb.app.minecraft.jsons.VersionManifest;
import dev.ftb.app.pack.CancellationToken;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by covers1624 on 28/1/22.
 */
public abstract class FabricInstallTask extends ModLoaderInstallTask {

    private static final String FABRIC_META = "https://meta.fabricmc.net/";
    private static final String QUILT_META = "https://meta.quiltmc.org/";

    private final String mcVersion;
    private final String loaderVersion;

    private final String versionName;

    private FabricInstallTask(String loaderName, String mcVersion, String loaderVersion) {
        this.mcVersion = mcVersion;
        this.loaderVersion = loaderVersion;

        versionName = loaderName + "-" + mcVersion + "-" + loaderVersion;
    }

    public static FabricInstallTask fabric(String mcVersion, String loaderVersion) {
        return new FabricInstallTask("fabric-loader", mcVersion, loaderVersion) {
            @Override
            protected DownloadTask getProfileDownload(Path dest) {
                return DownloadTask.builder()
                        .url(FABRIC_META + "v2/versions/loader/" + mcVersion + "/" + loaderVersion + "/profile/json")
                        .dest(dest)
                        .build();
            }
        };
    }

    public static FabricInstallTask quilt(String mcVersion, String loaderVersion) {
        return new FabricInstallTask("quilt-loader", mcVersion, loaderVersion) {
            @Override
            protected DownloadTask getProfileDownload(Path dest) {
                return DownloadTask.builder()
                        .url(QUILT_META + "v3/versions/loader/" + mcVersion + "/" + loaderVersion + "/profile/json")
                        .dest(dest)
                        .build();
            }
        };
    }

    @Override
    public void execute(@Nullable CancellationToken cancelToken, @Nullable TaskProgressListener listener) throws Throwable {
        Path versionsDir = Constants.BIN_LOCATION.resolve("versions");
        Path librariesDir = Constants.BIN_LOCATION.resolve("libraries");

        Path versionJson = versionsDir.resolve(versionName).resolve(versionName + ".json");

        VersionManifest loaderManifest = downloadLoaderManifest(versionJson, cancelToken);
        downloadVanilla(versionsDir, mcVersion);

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
        DownloadTask task = library.createDownloadTask(librariesDir, true);
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
