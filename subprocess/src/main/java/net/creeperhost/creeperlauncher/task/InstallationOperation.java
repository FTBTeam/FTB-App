package net.creeperhost.creeperlauncher.task;

import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.instances.InstallInstanceData;
import net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest;
import net.creeperhost.creeperlauncher.install.InstallProgressTracker;
import net.creeperhost.creeperlauncher.install.InstanceInstaller;
import net.creeperhost.creeperlauncher.pack.Instance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Created by covers1624 on 3/6/22.
 */
public class InstallationOperation extends LongRunningOperation {

    private static final Logger LOGGER = LogManager.getLogger();

    private final InstallInstanceData data;
    private final Instance instance;
    private final ModpackVersionManifest manifest;
    private final InstanceInstaller installer;

    public InstallationOperation(LongRunningTaskManager manager, InstallInstanceData data, Instance instance, ModpackVersionManifest manifest) throws IOException {
        super(manager);
        this.data = data;
        this.instance = instance;
        this.manifest = manifest;
        InstallProgressTracker tracker = new InstallProgressTracker(data);
        installer = new InstanceInstaller(instance, manifest, cancelToken, tracker);
    }

    public Instance getInstance() {
        return instance;
    }

    @Override
    protected void doOperation() throws Throwable {
        try {
            installer.prepare();
        } catch (Throwable ex) {
            throw new PrepareException(ex);
        }

        installer.execute();
    }

    @Override
    protected void onOperationException(Throwable ex) {
        if (ex instanceof PrepareException pex) {
            LOGGER.error("Fatal exception whilst preparing modpack installation.", pex);
            Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "error", "Fatal exception whilst preparing modpack installation."));
            return;
        }

        LOGGER.error("Fatal exception whilst installing modpack.", ex);
        Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "error", "Fatal exception whilst installing modpack."));
    }

    @Override
    protected void onComplete(CompletionReason reason) {
        switch (reason) {
            case NORMAL -> {
                Instances.addInstance(instance);
                Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "success", "Install complete.", instance.props));
            }
            case CANCELED -> Settings.webSocketAPI.sendMessage(new InstallInstanceData.Reply(data, "canceled", "Install canceled.", instance.props));
        }
    }

    private static class PrepareException extends RuntimeException {

        public PrepareException(Throwable ex) {
            super(ex);
        }
    }
}
