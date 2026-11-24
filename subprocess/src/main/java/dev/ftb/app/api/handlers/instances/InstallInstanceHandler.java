package dev.ftb.app.api.handlers.instances;

import dev.ftb.app.Analytics;
import dev.ftb.app.api.data.instances.InstallInstanceData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;
import dev.ftb.app.data.modpack.ModpackManifest;
import dev.ftb.app.data.modpack.ModpackVersionManifest;
import dev.ftb.app.pack.Instance;
import dev.ftb.app.pack.InstanceFactory;
import dev.ftb.app.task.InstallationOperation;
import dev.ftb.app.task.LongRunningTaskManager;
import dev.ftb.app.util.MiscUtils;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class InstallInstanceHandler implements IMessageHandler<InstallInstanceData> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public synchronized void handle(MessageContext context, InstallInstanceData data) {
        LongRunningTaskManager longTaskManager = context.services().threads().longRunningTaskManager();
        if (longTaskManager.anyTasksRunning(InstallationOperation.class)) {
            abort(context, data, "Installation already in progress");
            return;
        }

        try {
            if (StringUtils.isNotEmpty(data.uuid)) {
                handleUpdate(context, data);
            } else if (StringUtils.isNotEmpty(data.importFrom)) {
                handleCurseImport(context, data, data.importFrom);
            } else {
                handleNewInstall(context, data);
            }
        } catch (Throwable ex) {
            LOGGER.error("Error preparing Modpack installation.", ex);
            abort(context, data, "Fatal exception preparing installation task.");
        }
    }

    private void handleUpdate(MessageContext context, InstallInstanceData data) throws IOException {
        Instance instance = context.services().instances().getInstance(UUID.fromString(data.uuid));
        if (instance == null) {
            abort(context, data, "Install not started.");
            return;
        }

        Pair<ModpackManifest, ModpackVersionManifest> manifests = ModpackVersionManifest.queryManifests(
                context.services().http(),
                data.id,
                data.version,
                data._private,
                instance.props.packType
        );
        if (manifests == null) {
            abort(context, data, "Modpack not found");
            return;
        }
        beginInstallTask(context, data, instance, manifests.getRight());
    }

    private void handleCurseImport(MessageContext context, InstallInstanceData data, String importFrom) throws IOException {
        Path path = Paths.get(importFrom);
        if (Files.notExists(path)) {
            abort(context, data, "Modpack import file does not exist.");
            return;
        }
        if (!Files.isRegularFile(path)) {
            abort(context, data, "Modpack import file is not a file.");
            return;
        }
        Pair<ModpackManifest, ModpackVersionManifest> manifests = prepareCurseImport(context, path);
        if (manifests == null) {
            abort(context, data, "Failed to import curse pack.");
            return;
        }

        beginNewInstall(context, data, manifests.getLeft(), manifests.getRight(), false, (byte) 0, true);
    }

    public static void handleNewInstall(MessageContext context, InstallInstanceData data) throws IOException {
        Pair<ModpackManifest, ModpackVersionManifest> manifests = ModpackVersionManifest.queryManifests(
                context.services().http(),
                data.id,
                data.version,
                data._private,
                data.packType
        );
        if (manifests == null) {
            abort(context, data, "Modpack not found");
            return;
        }

        beginNewInstall(context, data, manifests.getLeft(), manifests.getRight(), data._private, data.packType, false);
    }

    private static void beginNewInstall(MessageContext context, InstallInstanceData data, ModpackManifest modpackManifest, ModpackVersionManifest versionManifest, boolean isPrivate, byte packType, boolean isImport) throws IOException {
        String mcVersion;
        if (versionManifest.getTargetCount("game") == 1) {
            mcVersion = versionManifest.getTargetVersion("game");
        } else {
            if (data.mcVersion == null) {
                abort(context, data, "Version manifest does not contain game version information. Must be manually specified.");
                return;
            }
            mcVersion = data.mcVersion;
        }
        
        var factory = InstanceFactory.fromServices(context.services());
        Instance instance = factory.create(data.name, data.artPath, MiscUtils.tryParseUuid(data.categoryId), modpackManifest, versionManifest, mcVersion, isPrivate, packType);
        
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
            Analytics.sendInstallRequest(context.services().http(), instance.getId(), instance.getVersionId(), instance.props.packType);
        }
        beginInstallTask(context, data, instance, versionManifest);
    }

    private static void beginInstallTask(MessageContext context, InstallInstanceData data, Instance instance, ModpackVersionManifest manifest) throws IOException {
        context.reply(new InstallInstanceData.Reply(data, "init", "Starting installation.", instance));

        var longTaskManager = context.services().threads().longRunningTaskManager();
        InstallationOperation op = new InstallationOperation(context.services(), longTaskManager, data, instance, manifest);
        op.submit();
    }

    private static void abort(MessageContext context, InstallInstanceData data, String reason) {
        context.reply(new InstallInstanceData.Reply(data, "prepare_error", reason));
    }

    @Nullable
    public static Pair<ModpackManifest, ModpackVersionManifest> prepareCurseImport(MessageContext context, Path curseZip) throws IOException {
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

        versionManifest = ModpackVersionManifest.convert(context.services().http(), inputData);
        if (versionManifest == null) {
            LOGGER.error("Failed to parse manifest.json from CurseForge zip: {}", curseZip);
            return null;
        }

        versionManifest.cfExtractOverride = curseZip;
        return Pair.of(ModpackManifest.fakeManifest(versionManifest.getName()), versionManifest);
    }
}
