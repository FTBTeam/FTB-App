package net.creeperhost.creeperlauncher.install.tasks.modloader;

import com.google.common.hash.Hashing;
import com.google.gson.JsonObject;
import net.covers1624.quack.gson.JsonUtils;
import net.covers1624.quack.io.IOUtils;
import net.covers1624.quack.maven.MavenNotation;
import net.covers1624.quack.util.HashUtils;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.data.forge.installerv1.InstallProfile;
import net.creeperhost.creeperlauncher.install.tasks.TaskProgressListener;
import net.creeperhost.creeperlauncher.pack.CancellationToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static java.util.Objects.requireNonNull;

/**
 * Handles the installation of a Forge V1 installer.
 * <p>
 * Unlike V2, this can pretty much just extract the universal jar and install the profile.
 * However, we make a good effort to install all libraries and necessary files during the installation process here.
 * <p>
 * Created by covers1624 on 24/1/22.
 */
public class ForgeV1InstallTask extends AbstractForgeInstallTask {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Path installerJar;

    @Nullable
    private String versionName;

    public ForgeV1InstallTask(Path installerJar) {
        this.installerJar = installerJar;
    }

    public static void main(String[] args) throws Throwable {
        System.setProperty("ftba.dataDirOverride", "./");
        new ForgeV1InstallTask(Path.of("forge-1.12.2-14.23.5.2838-installer.jar")).execute(null, null);
    }

    @Override
    public void execute(@Nullable CancellationToken cancelToken, @Nullable TaskProgressListener listener) throws Throwable {
        Path versionsDir = Constants.BIN_LOCATION.resolve("versions");
        Path librariesDir = Constants.BIN_LOCATION.resolve("libraries");

        try (FileSystem fs = IOUtils.getJarFileSystem(installerJar, true)) {
            Path installerRoot = fs.getPath("/");
            InstallProfile profile = JsonUtils.parse(InstallProfile.GSON, installerRoot.resolve("install_profile.json"), InstallProfile.class);
            if (profile.install.transform != null) throw new IOException("Unable to process Forge v1 Installer with transforms.");

            versionName = computeVersionName(profile.install.path);

            downloadVanilla(versionsDir, profile.install.minecraft);

            for (InstallProfile.Library library : profile.versionInfo.libraries) {
                if (library.clientreq == null || !library.clientreq) continue; // Skip, mirrors forge logic.
                Path libraryPath = processLibrary(cancelToken, installerRoot, librariesDir, library);

                if (library.checksums.isEmpty()) continue;

                String sha1 = HashUtils.hash(Hashing.sha1(), libraryPath).toString();

                if (!library.checksums.contains(sha1)) {
                    LOGGER.error("Failed to validate checksums of library {}. Expected one of {}. Got: {}",
                            library.name,
                            library.checksums,
                            sha1
                    );
                    // Some libraries in older v1 installers have changed. (scala iirc), just ignore these errors for now.
                    LOGGER.error("Continuing anyway..");
                }
            }

            LOGGER.info("Extracting {} from installer jar.", profile.install.path);
            Path universalPath = profile.install.path.toPath(librariesDir);
            Files.copy(installerRoot.resolve(profile.install.filePath), IOUtils.makeParents(universalPath), StandardCopyOption.REPLACE_EXISTING);

            JsonObject profileJson = JsonUtils.parse(InstallProfile.GSON, installerRoot.resolve("install_profile.json"), JsonObject.class);
            LOGGER.info("Writing version profile {}.", versionName);
            Path versionJson = versionsDir.resolve(versionName).resolve(versionName + ".json");
            JsonUtils.write(InstallProfile.GSON, IOUtils.makeParents(versionJson), profileJson.get("versionInfo"));
        }
    }

    @Nullable
    @Override
    public String getResult() {
        return versionName;
    }

    @VisibleForTesting
    static String computeVersionName(MavenNotation forgeNotation) {
        requireNonNull(forgeNotation.version);

        // It's weird, but mirrors the Forge v1 installer logic for this.
        // 1.12.2-forge1.12.2-14.23.5.2838
        int firstDash = forgeNotation.version.indexOf('-');
        return forgeNotation.version.substring(0, firstDash) + "-" + forgeNotation.module + forgeNotation.version;
    }
}
