package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.covers1624.quack.io.IOUtils;
import net.creeperhost.creeperlauncher.*;
import net.creeperhost.creeperlauncher.api.data.instances.InstallInstanceData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.data.modpack.ModpackManifest;
import net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest;
import net.creeperhost.creeperlauncher.data.modpack.ShareManifest;
import net.creeperhost.creeperlauncher.install.InstallProgressTracker;
import net.creeperhost.creeperlauncher.install.InstanceInstaller;
import net.creeperhost.creeperlauncher.pack.LocalInstance;
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
import java.util.concurrent.CompletableFuture;

public class InstallInstanceHandler implements IMessageHandler<InstallInstanceData> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(InstallInstanceData data) {
        if (data.shareCode != null) {
            LOGGER.debug("Received install request for shared pack: " + data.shareCode);
        } else {
            LOGGER.debug("Received install pack message for " + "ID:" + data.id + " VERSION:" + data.version + " PACKTYPE:" + data.packType);
        }
        if (CreeperLauncher.isInstalling) {
            assert CreeperLauncher.currentInstall != null;
            Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "error", "Install in progress.", CreeperLauncher.currentInstall.getInstance().getUuid().toString()));
            return;
        }
        CreeperLauncher.isInstalling = true;
        Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "init", "Install started.", ""));

        try {
            LocalInstance instance;
            if (StringUtils.isNotEmpty(data.uuid)) {
                instance = Instances.getInstance(UUID.fromString(data.uuid));
                Pair<ModpackManifest, ModpackVersionManifest> manifests = ModpackVersionManifest.queryManifests(
                        data.id,
                        data.version,
                        data._private,
                        instance.packType
                );
                if (manifests == null) {
                    Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "error", "Modpack not found", ""));
                    clearInstallState();
                    return;
                }
                handleInstall(data, instance, manifests.getRight());
            } else {
                String code = data.shareCode;
                ModpackManifest modpackManifest;
                ModpackVersionManifest versionManifest;
                boolean isPrivate;
                byte packType;
                boolean isImport = false;
                if (data.shareCode != null) {
                    ShareManifest shareManifest = ShareManifest.queryManifest(Constants.TRANSFER_HOST + code + "/manifest.json");
                    if (shareManifest == null) {
                        Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "error", "Unable to download manifest for '" + code + "', Share code may have expired.", null));
                        clearInstallState();
                        return;
                    }
                    versionManifest = shareManifest.getVersionManifest();
                    isPrivate = shareManifest.getType() == ShareManifest.Type.PRIVATE;
                    packType = shareManifest.getType() == ShareManifest.Type.CURSE ? (byte) 1 : 0;

                    modpackManifest = ModpackManifest.queryManifest(versionManifest.getParent(), isPrivate, packType);
                    if (modpackManifest == null) {
                        modpackManifest = ModpackManifest.queryManifest(versionManifest.getParent(), !isPrivate, packType);
                    }

                    if (modpackManifest == null) {
                        Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "error", "Unable to determine modpack for '" + code + "'.", null));
                        clearInstallState();
                        return;
                    }
                    // Run substitutions over manifest.
                    StrSubstitutor sub = new StrSubstitutor(Map.of("token", code));
                    for (ModpackVersionManifest.ModpackFile file : versionManifest.getFiles()) {
                        file.setUrl(sub.replace(file.getUrl()));
                    }
                }
                if (data.importFrom != null) {
                    Path importFrom = Paths.get(data.importFrom);
                    if (Files.notExists(importFrom)) {
                        Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "error", "Modpack import file does not exist.", ""));
                        clearInstallState();
                        return;
                    }
                    if (!Files.isRegularFile(importFrom)) {
                        Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "error", "Modpack import file is not a file.", ""));
                        clearInstallState();
                        return;
                    }
                    Pair<ModpackManifest, ModpackVersionManifest> manifests = prepareCurseImport(importFrom);
                    if (manifests == null) {
                        Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "error", "Failed to import curse pack.", ""));
                        clearInstallState();
                        return;
                    }
                    modpackManifest = manifests.getLeft();
                    versionManifest = manifests.getRight();
                    isImport = true;
                    isPrivate = false;
                    packType = (byte) 0;
                } else {
                    isPrivate = data._private;
                    packType = data.packType;
                    Pair<ModpackManifest, ModpackVersionManifest> manifests = ModpackVersionManifest.queryManifests(
                            data.id,
                            data.version,
                            data._private,
                            data.packType
                    );
                    if (manifests == null) {
                        Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "error", "Modpack not found", ""));
                        clearInstallState();
                        return;
                    }
                    modpackManifest = manifests.getLeft();
                    versionManifest = manifests.getRight();
                }
                instance = new LocalInstance(modpackManifest, versionManifest, isPrivate, packType);
                instance.isImport = isImport;
                Analytics.sendInstallRequest(instance.getId(), instance.getVersionId(), instance.packType);
                handleInstall(data, instance, versionManifest);
            }
        } catch (Throwable ex) {
            LOGGER.error("Fatal exception configuring modpack installation.", ex);
            Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "error", "Fatal exception configuring modpack installation.", ""));
            clearInstallState();
        }
    }

    private void handleInstall(InstallInstanceData data, LocalInstance instance, ModpackVersionManifest manifest) {
        try {
            InstallProgressTracker tracker = new InstallProgressTracker(data);
            InstanceInstaller installer = new InstanceInstaller(instance, manifest, tracker);
            CreeperLauncher.currentInstall = installer;
            installer.prepare();
            CreeperLauncher.currentInstallFuture = CompletableFuture.runAsync(() -> {
                try {
                    installer.execute();
                    Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "success", "Install complete.", instance.getUuid().toString()));
                    Instances.addInstance(instance.getUuid(), instance);
                } catch (InstanceInstaller.InstallationFailureException ex) {
                    LOGGER.error("Fatal exception whilst installing modpack.", ex);
                    Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "error", "Fatal exception whilst installing modpack.", ""));
                }
                clearInstallState();
            });
        } catch (IOException ex) {
            LOGGER.error("Fatal exception preparing modpack installation.", ex);
            Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "error", "Fatal exception whilst preparing modpack installation.", ""));
            clearInstallState();
        }
    }

    private static void clearInstallState() {
        CreeperLauncher.isInstalling = false;
        CreeperLauncher.currentInstall = null;
        CreeperLauncher.currentInstallFuture = null;
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
