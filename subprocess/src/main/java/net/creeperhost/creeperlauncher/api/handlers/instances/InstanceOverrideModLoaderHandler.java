package net.creeperhost.creeperlauncher.api.handlers.instances;

import com.google.gson.JsonSyntaxException;
import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.instances.InstanceOverrideModLoaderData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.data.InstanceModifications;
import net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest;
import net.creeperhost.creeperlauncher.install.tasks.modloader.ModLoaderInstallTask;
import net.creeperhost.creeperlauncher.pack.Instance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Created by covers1624 on 6/9/23.
 */
public class InstanceOverrideModLoaderHandler implements IMessageHandler<InstanceOverrideModLoaderData> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(InstanceOverrideModLoaderData data) {
        Instance instance = Instances.getInstance(data.uuid);
        if (instance == null) {
            Settings.webSocketAPI.sendMessage(new InstanceOverrideModLoaderData.Reply(data, "error", "Instance does not exist."));
            return;
        }

        // TODO -1 to set disable override.

        ModpackVersionManifest versionManifest;
        try {
            versionManifest = ModpackVersionManifest.queryManifest(data.modLoaderId, data.modLoaderVersion, false, (byte) 0);
        } catch (IOException | JsonSyntaxException ex) {
            LOGGER.error("Error Finding ModLoader Manifest.", ex);
            Settings.webSocketAPI.sendMessage(new InstanceOverrideModLoaderData.Reply(data, "error", "Error requesting ModLoader version."));
            return;
        }
        if (versionManifest == null) {
            Settings.webSocketAPI.sendMessage(new InstanceOverrideModLoaderData.Reply(data, "error", "ModLoader version does not exist."));
            return;
        }

        ModpackVersionManifest.Target target = versionManifest.findTarget("modloader");
        if (target == null) {
            Settings.webSocketAPI.sendMessage(new InstanceOverrideModLoaderData.Reply(data, "error", "Version does not have ModLoader target??"));
            return;
        }

        // TODO, do this in the background? Replace with new generic progress system?
        try {
            InstanceModifications modifications = instance.getOrCreateModifications();
            modifications.setModLoaderOverride(target);

            Settings.webSocketAPI.sendMessage(new InstanceOverrideModLoaderData.Reply(data, "prepare", "Preparing ModLoader install."));

            ModLoaderInstallTask task = ModLoaderInstallTask.createInstallTask(
                    instance,
                    instance.getMcVersion(),
                    target.getName(),
                    target.getVersion()
            );

            task.execute(null, null);
            instance.props.modLoader = task.getResult();
            instance.saveJson();
            instance.saveModifications();
            Settings.webSocketAPI.sendMessage(new InstanceOverrideModLoaderData.Reply(data, "success", "Installed new ModLoader version."));
        } catch (Throwable ex) {
            Settings.webSocketAPI.sendMessage(new InstanceOverrideModLoaderData.Reply(data, "error", "Error installing ModLoader version."));
        }

    }
}
