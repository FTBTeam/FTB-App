package dev.ftb.app.install.tasks.modloader.forge;

import dev.ftb.app.AppMain;
import dev.ftb.app.Constants;
import dev.ftb.app.install.tasks.DownloadTask;
import dev.ftb.app.install.tasks.DownloadTask.DownloadValidation;
import dev.ftb.app.install.tasks.TaskProgressListener;
import dev.ftb.app.minecraft.jsons.VersionManifest;
import dev.ftb.app.pack.CancellationToken;
import dev.ftb.app.pack.Instance;
import net.covers1624.quack.maven.MavenNotation;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.apache.commons.lang3.StringUtils.appendIfMissing;

/**
 * Forge 'jarmod' versions.
 * <p>
 */
// TODO We control the entire launch process now, this can be done _very_ differently.
public class LegacyForgeInstallTask extends AbstractForgeInstallTask {

    private final Instance instance;
    private final String mcVersion;
    private final String forgeVersion;

    public LegacyForgeInstallTask(Instance instance, String mcVersion, String forgeVersion) {
        this.instance = instance;
        this.mcVersion = mcVersion;
        this.forgeVersion = forgeVersion;
        versionName = mcVersion + "-forge" + mcVersion + "-" + forgeVersion;
    }

    @Override
    public void execute(@Nullable CancellationToken cancelToken, @Nullable TaskProgressListener listener) throws Throwable {
        assert versionName != null;

        instance.props.hasInstMods = true;
        Path versionsDir = AppMain.paths().mcVersionsDir();
        Path instMods = instance.getDir().resolve("instmods");
        
        instance.props.jvmArgs = instance.props.jvmArgs + " -Dfml.ignorePatchDiscrepancies=true -Dfml.ignoreInvalidMinecraftCertificates=true -Dminecraft.applet.TargetDirectory=\"" + instance.getDir().toAbsolutePath() + "\"";

        if (cancelToken != null) cancelToken.throwIfCancelled();

        MavenNotation universal = getForgeNotation(mcVersion, forgeVersion);
        DownloadTask dlForge = DownloadTask.builder()
                .url(appendIfMissing(Constants.CH_MAVEN, "/") + universal.toPath())
                .dest(instMods.resolve(versionName + ".jar"))
                .build();
        if (!dlForge.isRedundant()) {
            dlForge.execute(cancelToken, null);
        }

        VersionManifest vanillaManifest = downloadVanilla(versionsDir, mcVersion);

        Files.copy(
                versionsDir.resolve(vanillaManifest.id).resolve(vanillaManifest.id + ".jar"),
                instMods.resolve("minecraft.jar"),
                StandardCopyOption.REPLACE_EXISTING
        );

        DownloadTask dlForgeVersionJson = DownloadTask.builder()
                .url(Constants.MC_JSONS + "forge-" + mcVersion + ".json")
                .dest(versionsDir.resolve(versionName).resolve(versionName + ".json"))
                .withValidation(DownloadValidation.of().withUseETag(true).withUseOnlyIfModified(true))
                .build();
        if (!dlForgeVersionJson.isRedundant()) {
            dlForgeVersionJson.execute(cancelToken, null);
        }

        if (cancelToken != null) cancelToken.throwIfCancelled();

        ForgeLegacyLibraryHelper.installLegacyLibs(cancelToken, instance, mcVersion);
    }
}
