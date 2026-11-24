package dev.ftb.app.task;

import dev.ftb.app.api.data.instances.InstallInstanceData;
import dev.ftb.app.data.modpack.ModpackVersionManifest;
import dev.ftb.app.install.InstanceInstaller;
import dev.ftb.app.install.OperationProgressTracker;
import dev.ftb.app.pack.Instance;
import dev.ftb.app.services.Services;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Map;

public class InstallationOperation extends LongRunningOperation {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Services services;
    
    private final InstallInstanceData data;
    private final Instance instance;
    private final ModpackVersionManifest manifest;
    private final InstanceInstaller installer;

    // This class is only created from the webhook api meaning we can pass services. 
    // If it is ever used anywhere else we would need to refactor this and pass the services respectively.
    public InstallationOperation(Services services, LongRunningTaskManager manager, InstallInstanceData data, Instance instance, ModpackVersionManifest manifest) throws IOException {
        super(manager);
        this.services = services;
        this.data = data;
        this.instance = instance;
        this.manifest = manifest;
        OperationProgressTracker tracker = new OperationProgressTracker(services.rcpApi(), "install", Map.of("instance", instance.getUuid().toString()));
        installer = new InstanceInstaller(services.paths(), services.settings(), services.threads(), services.cache(), services.http(), instance, manifest, cancelToken, tracker);
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
            this.services.rcpApi().sendMessage(new InstallInstanceData.Reply(data, "error", "Fatal exception whilst preparing modpack installation."));
            return;
        }

        LOGGER.error("Fatal exception whilst installing modpack.", ex);
        this.services.rcpApi().sendMessage(new InstallInstanceData.Reply(data, "error", "Fatal exception whilst installing modpack."));
    }

    @Override
    protected void onComplete(CompletionReason reason) {
        switch (reason) {
            case NORMAL -> {
                this.services.instances().addInstance(instance);
                this.services.rcpApi().sendMessage(new InstallInstanceData.Reply(data, "success", "Install complete.", instance));
            }
            case CANCELED -> this.services.rcpApi().sendMessage(new InstallInstanceData.Reply(data, "canceled", "Install canceled.", instance));
        }
    }

    private static class PrepareException extends RuntimeException {

        public PrepareException(Throwable ex) {
            super(ex);
        }
    }
}
