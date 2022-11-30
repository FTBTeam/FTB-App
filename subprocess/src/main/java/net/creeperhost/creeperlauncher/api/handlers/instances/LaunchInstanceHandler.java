package net.creeperhost.creeperlauncher.api.handlers.instances;

import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.instances.LaunchInstanceData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.pack.CancellationToken;
import net.creeperhost.creeperlauncher.pack.InstanceLaunchException;
import net.creeperhost.creeperlauncher.pack.InstanceLauncher;
import net.creeperhost.creeperlauncher.pack.LocalInstance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public class LaunchInstanceHandler implements IMessageHandler<LaunchInstanceData> {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void handle(LaunchInstanceData data) {
        String _uuid = data.uuid;
        UUID uuid = UUID.fromString(_uuid);
        LocalInstance instance = Instances.getInstance(uuid);
        InstanceLauncher launcher = instance.getLauncher();

        if (data.cancelLaunch && instance.prepareFuture != null && instance.prepareToken != null) {
            instance.prepareToken.cancel(instance.prepareFuture);
            Settings.webSocketAPI.sendMessage(new LaunchInstanceData.Reply(data, "canceled", "Instance launch canceled."));
            return;
        }

        if (launcher.isRunning()) {
            Settings.webSocketAPI.sendMessage(new LaunchInstanceData.Reply(data, "error", "Instance is already running."));
            return;
        }

        assert instance.prepareFuture == null;
        assert instance.prepareToken == null;

        instance.prepareToken = new CancellationToken();
        instance.prepareFuture = CompletableFuture.runAsync(() -> {
            try {
                instance.play(instance.prepareToken, data.extraArgs, data.offline ? requireNonNull(data.username, "Username not specified for offline launch") : null);
                Settings.webSocketAPI.sendMessage(new LaunchInstanceData.Reply(data, "success", ""));
            } catch (InstanceLaunchException ex) {
                if (ex instanceof InstanceLaunchException.Abort) {
                    Settings.webSocketAPI.sendMessage(new LaunchInstanceData.Reply(data, "abort", ex.getMessage()));
                } else {
                    LOGGER.error("Failed to launch instance.", ex);

                    String message = ex.getMessage();
                    Throwable cause = ex.getCause();
                    if (cause != null) {
                        message += " because.. " + cause.getClass().getName() + ": " + cause.getMessage();
                    }

                    Settings.webSocketAPI.sendMessage(new LaunchInstanceData.Reply(data, "error", message));
                }
            }
            instance.prepareToken = null;
            instance.prepareFuture = null;
        });
    }
}
