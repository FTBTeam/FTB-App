package dev.ftb.app.api.handlers.instances;

import dev.ftb.app.Analytics;
import dev.ftb.app.AppMain;
import dev.ftb.app.Instances;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.instances.InstallInstanceData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.data.modpack.ModpackManifest;
import dev.ftb.app.data.modpack.ModpackVersionManifest;
import dev.ftb.app.pack.Instance;
import dev.ftb.app.task.InstallationOperation;
import dev.ftb.app.util.MiscUtils;
import net.covers1624.quack.io.IOUtils;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class InstallInstanceHandler implements IMessageHandler<InstallInstanceData> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public synchronized void handle(InstallInstanceData data) {
        if (AppMain.LONG_TASK_MANAGER.anyTasksRunning(InstallationOperation.class)) {
            abort(data, "Installation already in progress");
            return;
        }

        try {
            if (StringUtils.isNotEmpty(data.uuid)) {
                handleUpdate(data);
            } else if (StringUtils.isNotEmpty(data.importFrom)) {
                handleCurseImport(data, data.importFrom);
            } else {
                handleNewInstall(data);
            }
        } catch (Throwable ex) {
            LOGGER.error("Error preparing Modpack installation.", ex);
            abort(data, "Fatal exception preparing installation task.");
        }
    }

    private void handleUpdate(InstallInstanceData data) throws IOException {
        Instance instance = Instances.getInstance(UUID.fromString(data.uuid));
        if (instance == null) {
            abort(data, "Install not started.");
            return;
        }

        Pair<ModpackManifest, ModpackVersionManifest> manifests = ModpackVersionManifest.queryManifests(
                data.id,
                data.version,
                data._private,
                instance.props.packType
        );
        if (manifests == null) {
            abort(data, "Modpack not found");
            return;
        }
        beginInstallTask(data, instance, manifests.getRight());
    }

    private void handleCurseImport(InstallInstanceData data, String importFrom) throws IOException {
        Path path = Paths.get(importFrom);
        if (Files.notExists(path)) {
            abort(data, "Modpack import file does not exist.");
            return;
        }
        if (!Files.isRegularFile(path)) {
            abort(data, "Modpack import file is not a file.");
            return;
        }
        Pair<ModpackManifest, ModpackVersionManifest> manifests = prepareCurseImport(path);
        if (manifests == null) {
            abort(data, "Failed to import curse pack.");
            return;
        }

        beginNewInstall(data, manifests.getLeft(), manifests.getRight(), false, (byte) 0, true);
    }

    public static void handleNewInstall(InstallInstanceData data) throws IOException {
        Pair<ModpackManifest, ModpackVersionManifest> manifests = ModpackVersionManifest.queryManifests(
                data.id,
                data.version,
                data._private,
                data.packType
        );
        if (manifests == null) {
            abort(data, "Modpack not found");
            return;
        }

        beginNewInstall(data, manifests.getLeft(), manifests.getRight(), data._private, data.packType, false);
    }

    private static void beginNewInstall(InstallInstanceData data, ModpackManifest modpackManifest, ModpackVersionManifest versionManifest, boolean isPrivate, byte packType, boolean isImport) throws IOException {
        String mcVersion;
        if (versionManifest.getTargetCount("game") == 1) {
            mcVersion = versionManifest.getTargetVersion("game");
        } else {
            if (data.mcVersion == null) {
                abort(data, "Version manifest does not contain game version information. Must be manually specified.");
                return;
            }
            mcVersion = data.mcVersion;
        }
        
        Instance instance = new Instance(data.name, data.artPath, MiscUtils.tryParseUuid(data.categoryId), modpackManifest, versionManifest, mcVersion, isPrivate, packType);
        instance.props.isImport = isImport;
        if (isImport) {
            instance.props.locked = false;
        }
        
        // If the instance is our own, we shouldn't lock it by default.
        if (data.ourOwn) {
            instance.props.locked = false;
        }
        
        if (data.fullscreen != null && data.fullscreen) instance.props.fullscreen = true;
        if (data.ram != -1) instance.props.memory = data.ram;
        if (data.screenWidth != -1) instance.props.width = data.screenWidth;
        if (data.screenHeight != -1) instance.props.height = data.screenHeight;
        
        if (instance.getId() != -1 && instance.getVersionId() != -1) {
            Analytics.sendInstallRequest(instance.getId(), instance.getVersionId(), instance.props.packType);
        }
        beginInstallTask(data, instance, versionManifest);
    }

    private static void beginInstallTask(InstallInstanceData data, Instance instance, ModpackVersionManifest manifest) throws IOException {
        WebSocketHandler.sendMessage(new InstallInstanceData.Reply(data, "init", "Starting installation.", instance));

        InstallationOperation op = new InstallationOperation(AppMain.LONG_TASK_MANAGER, data, instance, manifest);
        op.submit();
    }

    private static void abort(InstallInstanceData data, String reason) {
        WebSocketHandler.sendMessage(new InstallInstanceData.Reply(data, "prepare_error", reason));
    }

    @Nullable
    public static Pair<ModpackManifest, ModpackVersionManifest> prepareCurseImport(Path curseZip) throws IOException {
        ModpackVersionManifest versionManifest;

        String inputData = null;
        try (ZipFile zipFile = new ZipFile.Builder().setFile(curseZip.toFile()).get()) {
            var entries = zipFile.getEntries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (entry.getName().equals("manifest.json")) {
                    try (InputStream inputStream = zipFile.getInputStream(entry)) {
                        inputData = new String(inputStream.readAllBytes());
                    } catch (IOException e) {
                        LOGGER.error("Failed to read manifest.json from CurseForge zip.", e);
                    }
                    
                    break;
                }
            }
        }
        
        if (inputData == null) {
            LOGGER.error("No manifest.json found in CurseForge zip: {}", curseZip);
            return null;
        }

        versionManifest = ModpackVersionManifest.convert(inputData);
        if (versionManifest == null) {
            LOGGER.error("Failed to parse manifest.json from CurseForge zip: {}", curseZip);
            return null;
        }

        versionManifest.cfExtractOverride = curseZip;
        return Pair.of(ModpackManifest.fakeManifest(versionManifest.getName()), versionManifest);
    }
}
