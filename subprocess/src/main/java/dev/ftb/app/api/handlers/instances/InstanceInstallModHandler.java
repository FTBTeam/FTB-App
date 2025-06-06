package dev.ftb.app.api.handlers.instances;

import net.covers1624.quack.collection.FastStream;
import dev.ftb.app.Instances;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.instances.InstanceInstallModData;
import dev.ftb.app.api.data.instances.InstanceInstallModData.PendingInstall;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.data.InstanceModifications;
import dev.ftb.app.data.modpack.ModpackVersionManifest;
import dev.ftb.app.install.ModInstaller;
import dev.ftb.app.pack.Instance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class InstanceInstallModHandler implements IMessageHandler<InstanceInstallModData> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(InstanceInstallModData data) {
        Instance instance = Instances.getInstance(data.uuid);
        if (instance == null) {
            WebSocketHandler.sendMessage(new InstanceInstallModData.Reply(data, "error", "Instance does not exist."));
            return;
        }

        String mcVersion = instance.getMcVersion();
        if (mcVersion == null) {
            WebSocketHandler.sendMessage(new InstanceInstallModData.Reply(data, "error", "Instance does not have a game version??"));
            return;
        }

        InstanceModifications modifications = instance.getModifications();
        var modLoader = modifications != null ? modifications.getModLoaderOverride() : null;
        String modloaderName;
        if (modLoader == null) {
            modloaderName = instance.resolveBasicModLoader();
        } else {
            modloaderName = modLoader.getName();
        }

        if (modloaderName == null || modloaderName.isEmpty()) {
            WebSocketHandler.sendMessage(new InstanceInstallModData.Reply(data, "error", "Instance does not have a ModLoader installed."));
            return;
        }

        ModInstaller modInstaller = new ModInstaller(instance, mcVersion, modloaderName, data.modId, data.versionId);

        try {
            modInstaller.resolve();
        } catch (ModInstaller.ModInstallerException ex) {
            LOGGER.warn("Error whilst preparing Mod install.", ex);
            WebSocketHandler.sendMessage(new InstanceInstallModData.Reply(data, "error", ex.getMessage()));
            return;
        }

        // TODO, ModInstaller actually has the unavailable/unsatisifable dependency list available. We should tell the user this.

        List<PendingInstall> pending = FastStream.of(modInstaller.getToInstall())
                .map(e -> new PendingInstall(e.getKey().getId(), e.getValue().getId()))
                .toList();
        WebSocketHandler.sendMessage(new InstanceInstallModData.Reply(data, "processing", "Processing install.", pending));

        try {
            modInstaller.install();
            WebSocketHandler.sendMessage(new InstanceInstallModData.Reply(data, "success", "Install successful."));
        } catch (ModInstaller.ModInstallerException ex) {
            LOGGER.warn("Error whilst installing mods.", ex);
            WebSocketHandler.sendMessage(new InstanceInstallModData.Reply(data, "error", ex.getMessage()));
        }
    }
}
