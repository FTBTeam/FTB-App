package dev.ftb.app.task;

import dev.ftb.app.Instances;
import dev.ftb.app.api.WebSocketHandler;
import dev.ftb.app.api.data.instances.InstallInstanceData;
import dev.ftb.app.data.modpack.ModpackVersionManifest;
import dev.ftb.app.install.InstanceInstaller;
import dev.ftb.app.install.OperationProgressTracker;
import dev.ftb.app.pack.Instance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Map;

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
        OperationProgressTracker tracker = new OperationProgressTracker("install", Map.of("instance", instance.getUuid().toString()));
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
            WebSocketHandler.sendMessage(new InstallInstanceData.Reply(data, "error", "Fatal exception whilst preparing modpack installation."));
            return;
        }

        LOGGER.error("Fatal exception whilst installing modpack.", ex);
        WebSocketHandler.sendMessage(new InstallInstanceData.Reply(data, "error", "Fatal exception whilst installing modpack."));
    }

    @Override
    protected void onComplete(CompletionReason reason) {
        switch (reason) {
            case NORMAL -> {
                Instances.addInstance(instance);
                WebSocketHandler.sendMessage(new InstallInstanceData.Reply(data, "success", "Install complete.", instance));
            }
            case CANCELED -> WebSocketHandler.sendMessage(new InstallInstanceData.Reply(data, "canceled", "Install canceled.", instance));
        }
    }

    private static class PrepareException extends RuntimeException {

        public PrepareException(Throwable ex) {
            super(ex);
        }
    }
}
