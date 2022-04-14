package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.Analytics;
import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.instances.InstallInstanceData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.data.modpack.ModpackManifest;
import net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest;
import net.creeperhost.creeperlauncher.install.InstallProgressTracker;
import net.creeperhost.creeperlauncher.install.InstanceInstaller;
import net.creeperhost.creeperlauncher.pack.LocalInstance;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class InstallInstanceHandler implements IMessageHandler<InstallInstanceData> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(InstallInstanceData data) {
        LOGGER.debug("Received install pack message for " + "ID:" + data.id + " VERSION:" + data.version + " PACKTYPE:" + data.packType);
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
                instance = new LocalInstance(manifests.getLeft(), manifests.getRight(), data._private, data.packType);
                Analytics.sendInstallRequest(instance.getId(), instance.getVersionId(), instance.packType);
                handleInstall(data, instance, manifests.getRight());
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
}
