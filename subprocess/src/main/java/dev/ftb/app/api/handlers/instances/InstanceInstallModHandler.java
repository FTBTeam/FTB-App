package dev.ftb.app.api.handlers.instances;

import dev.ftb.app.api.data.instances.InstanceInstallModData;
import dev.ftb.app.api.data.instances.InstanceInstallModData.PendingInstall;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;
import dev.ftb.app.data.InstanceModifications;
import dev.ftb.app.install.ModInstaller;
import dev.ftb.app.pack.Instance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class InstanceInstallModHandler implements IMessageHandler<InstanceInstallModData> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(MessageContext context, InstanceInstallModData data) {
        Instance instance = context.services().instances().getInstance(data.uuid);
        if (instance == null) {
            context.reply(new InstanceInstallModData.Reply(data, "error", "Instance does not exist."));
            return;
        }

        String mcVersion = instance.getMcVersion();
        if (mcVersion == null) {
            context.reply(new InstanceInstallModData.Reply(data, "error", "Instance does not have a game version??"));
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
            context.reply(new InstanceInstallModData.Reply(data, "error", "Instance does not have a ModLoader installed."));
            return;
        }

        ModInstaller modInstaller = new ModInstaller(context.services().http(), context.services().rcpApi(), context.services().threads(), instance, context.services().cache(), mcVersion, modloaderName, data.modId, data.versionId);

        try {
            modInstaller.resolve();
        } catch (ModInstaller.ModInstallerException ex) {
            LOGGER.warn("Error whilst preparing Mod install.", ex);
            context.reply(new InstanceInstallModData.Reply(data, "error", ex.getMessage()));
            return;
        }

        // TODO, ModInstaller actually has the unavailable/unsatisifable dependency list available. We should tell the user this.

        List<PendingInstall> pending = modInstaller.getToInstall().stream()
                .map(e -> new PendingInstall(e.getKey().getId(), e.getValue().getId()))
                .toList();
        context.reply(new InstanceInstallModData.Reply(data, "processing", "Processing install.", pending));

        try {
            modInstaller.install();
            context.reply(new InstanceInstallModData.Reply(data, "success", "Install successful."));
        } catch (ModInstaller.ModInstallerException ex) {
            LOGGER.warn("Error whilst installing mods.", ex);
            context.reply(new InstanceInstallModData.Reply(data, "error", ex.getMessage()));
        }
    }
}
