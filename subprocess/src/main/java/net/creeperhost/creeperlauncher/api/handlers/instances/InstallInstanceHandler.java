package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.covers1624.quack.io.IOUtils;
import net.creeperhost.creeperlauncher.*;
import net.creeperhost.creeperlauncher.api.data.instances.InstallInstanceData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.data.modpack.ModpackManifest;
import net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest;
import net.creeperhost.creeperlauncher.data.modpack.ShareManifest;
import net.creeperhost.creeperlauncher.pack.Instance;
import net.creeperhost.creeperlauncher.task.InstallationOperation;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

public class InstallInstanceHandler implements IMessageHandler<InstallInstanceData> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public synchronized void handle(InstallInstanceData data) {
        if (CreeperLauncher.LONG_TASK_MANAGER.anyTasksRunning(InstallationOperation.class)) {
            abort(data, "Installation already in progress");
            return;
        }

        try {
            if (StringUtils.isNotEmpty(data.uuid)) {
                handleUpdate(data);
            } else if (StringUtils.isNotEmpty(data.shareCode)) {
                handleShareImport(data, data.shareCode);
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

    private void handleShareImport(InstallInstanceData data, String shareCode) throws IOException {
        ShareManifest shareManifest = ShareManifest.queryManifest(Constants.TRANSFER_HOST + shareCode + "/manifest.json");
        if (shareManifest == null) {
            abort(data, "Unable to download manifest for '" + shareCode + "', Share code may have expired.");
            return;
        }

        ModpackVersionManifest versionManifest = shareManifest.getVersionManifest();
        boolean isPrivate = shareManifest.getType() == ShareManifest.Type.PRIVATE;
        byte packType = shareManifest.getType() == ShareManifest.Type.CURSE ? (byte) 1 : 0;
        boolean isImport = shareManifest.getType() == ShareManifest.Type.IMPORT;

        ModpackManifest modpackManifest;
        if (isImport) {
            modpackManifest = ModpackManifest.fakeManifest(shareManifest.getName());
        } else {
            modpackManifest = ModpackManifest.queryManifest(versionManifest.getParent(), isPrivate, packType);
            if (modpackManifest == null) {
                modpackManifest = ModpackManifest.queryManifest(versionManifest.getParent(), !isPrivate, packType);
            }

            if (modpackManifest == null) {
                abort(data, "Unable to determine modpack for '" + shareCode + "'.");
                return;
            }
        }
        // Run substitutions over manifest.
        StrSubstitutor sub = new StrSubstitutor(Map.of("token", shareCode));
        for (ModpackVersionManifest.ModpackFile file : versionManifest.getFiles()) {
            file.setUrl(sub.replace(file.getUrl()));
        }

        beginNewInstall(data, modpackManifest, versionManifest, isPrivate, packType, false);
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
        Instance instance = new Instance(data.name, data.artPath, data.category, modpackManifest, versionManifest, isPrivate, packType);
        instance.props.isImport = isImport;
        if (instance.getId() != -1 && instance.getVersionId() != -1) {
            Analytics.sendInstallRequest(instance.getId(), instance.getVersionId(), instance.props.packType);
        }
        beginInstallTask(data, instance, versionManifest);
    }

    private static void beginInstallTask(InstallInstanceData data, Instance instance, ModpackVersionManifest manifest) throws IOException {
        Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "init", "Starting installation.", instance.props));

        InstallationOperation op = new InstallationOperation(CreeperLauncher.LONG_TASK_MANAGER, data, instance, manifest);
        op.submit();
    }

    private static void abort(InstallInstanceData data, String reason) {
        Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "prepare_error", reason));
    }

    @Nullable
    public static Pair<ModpackManifest, ModpackVersionManifest> prepareCurseImport(Path curseZip) throws IOException {
        ModpackVersionManifest versionManifest;
        try (FileSystem fs = IOUtils.getJarFileSystem(curseZip, true)) {
            Path manifestJson = fs.getPath("/manifest.json");
            versionManifest = ModpackVersionManifest.convert(manifestJson);
            if (versionManifest == null) {
                return null;
            }
            versionManifest.cfExtractOverride = curseZip;
            return Pair.of(ModpackManifest.fakeManifest(versionManifest.getName()), versionManifest);
        }
    }
}
