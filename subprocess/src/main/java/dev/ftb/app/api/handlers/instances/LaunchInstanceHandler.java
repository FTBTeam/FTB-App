package dev.ftb.app.api.handlers.instances;

import dev.ftb.app.api.data.instances.LaunchInstanceData;
import dev.ftb.app.api.handlers.IMessageHandler;
import dev.ftb.app.api.handlers.MessageContext;
import dev.ftb.app.pack.CancellationToken;
import dev.ftb.app.pack.Instance;
import dev.ftb.app.pack.InstanceLaunchException;
import dev.ftb.app.pack.InstanceLauncher;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LaunchInstanceHandler implements IMessageHandler<LaunchInstanceData> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(MessageContext context, LaunchInstanceData data) {
        String _uuid = data.uuid;
        UUID uuid = UUID.fromString(_uuid);
        Instance instance = context.services().instances().getInstance(uuid);
        if (instance == null) {
            context.reply(new LaunchInstanceData.Reply(data, uuid, "error", "Instance does not exist."));
            return;
        }
        
        InstanceLauncher launcher = instance.getLauncher();

        if (data.cancelLaunch && instance.prepareFuture != null && instance.prepareToken != null) {
            instance.prepareToken.cancel(instance.prepareFuture);
            context.reply(new LaunchInstanceData.Reply(data, uuid, "canceled", "Instance launch canceled."));
            return;
        }

        if (launcher.isRunning()) {
            context.reply(new LaunchInstanceData.Reply(data, uuid, "error", "Instance is already running."));
            return;
        }
        if (data.offline && StringUtils.isEmpty(data.offlineUsername)) {
            context.reply(new LaunchInstanceData.Reply(data, uuid, "error", "Offline username not specified."));
            return;
        }

        assert instance.prepareFuture == null;
        assert instance.prepareToken == null;

        instance.prepareToken = new CancellationToken();
        instance.prepareFuture = CompletableFuture.runAsync(() -> {
            LOGGER.info("Preparing instance for launch...");
            try {
                instance.play(instance.prepareToken, data.extraArgs, data.offline ? data.offlineUsername : null);
                context.reply(new LaunchInstanceData.Reply(data, uuid, "success", ""));
            } catch (InstanceLaunchException ex) {
                if (ex instanceof InstanceLaunchException.Abort) {
                    context.reply(new LaunchInstanceData.Reply(data, uuid, "abort", ex.getMessage()));
                } else {
                    LOGGER.error("Failed to launch instance.", ex);
                    Throwable cause = ex.getCause();
//                    Sentry.addBreadcrumb(ex.getMessage());
                    LOGGER.error("Failed to launch instance.", cause != null ? cause : ex);

                    String message = ex.getMessage();
                    if (cause != null) {
                        message += " because.. " + cause.getClass().getName() + ": " + cause.getMessage();
                    }

                    context.reply(new LaunchInstanceData.Reply(data, uuid, "error", message));
                }
            } catch (Throwable ex) {
                LOGGER.error("Unknown error whilst starting instance.", ex);
                context.reply(new LaunchInstanceData.Reply(data, uuid, "error", "Unexpected error whilst starting instance."));
            }
            instance.prepareToken = null;
            instance.prepareFuture = null;
        }, context.services().threads().instanceLauncherPool());
    }
}
