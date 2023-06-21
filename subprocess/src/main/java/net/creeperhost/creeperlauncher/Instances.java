package net.creeperhost.creeperlauncher;

import net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest;
import net.creeperhost.creeperlauncher.pack.Instance;
import net.creeperhost.creeperlauncher.util.ElapsedTimer;
import net.creeperhost.creeperlauncher.util.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Instances {

    private static final Logger LOGGER = LogManager.getLogger();

    private static Map<UUID, Instance> instances = new HashMap<>();

    @Nullable
    public static Instance getInstance(UUID uuid) {
        return Instances.instances.get(uuid);
    }

    public static void addInstance(Instance instance) {
        instances.put(instance.getUuid(), instance);
    }

    public static Collection<Instance> allInstances() {
        return Collections.unmodifiableCollection(Instances.instances.values());
    }

    public static void refreshInstances() {
        ElapsedTimer totalTimer = new ElapsedTimer();
        Path instancesDir = Settings.getInstancesDir();

        LOGGER.info("Reloading instances..");
        instances.clear();

        if (!Files.exists(instancesDir)) {
            LOGGER.info("Instances directory missing, skipping..");
        } else {
            ElapsedTimer timer = new ElapsedTimer();
            List<Instance> loadedInstances = FileUtils.listDir(instancesDir)
                    .parallelStream()
                    .filter(e -> !e.getFileName().toString().startsWith("."))
                    .map(Instances::loadInstance)
                    .filter(Objects::nonNull)
                    .toList();
            instances = new HashMap<>();
            for (Instance instance : loadedInstances) {
                // TODO, there is probably a better solution to this.
                if (instances.containsKey(instance.getUuid())) {
                    LOGGER.warn("Found duplicate instance {} with id {}. Ignoring.", instance.getDir(), instance.getUuid());
                    continue;
                }
                instances.put(instance.getUuid(), instance);
            }
            LOGGER.info("Loaded {} out of {} instances in {}.", instances.size(), loadedInstances.size(), timer.elapsedStr());
        }

        CreeperLauncher.CLOUD_SAVE_MANAGER.pollCloudInstances();

        LOGGER.info("Finished instance reload in {}", totalTimer.elapsedStr());
    }

    private static Instance loadInstance(Path path) {
        Path json = path.resolve("instance.json");
        if (Files.notExists(json)) {
            LOGGER.warn("Instance missing 'instance.json', Ignoring. {}", json.toAbsolutePath());
            return null;
        }
        try {
            Instance localInstance = new Instance(path, json);
            if (!localInstance.props.installComplete) {
                // TODO we should provide a cleanup function somewhere to remove these old installs, probably next to our cache flush button.
                LOGGER.warn("Instance install never completed, Ignoring. {}", json.toAbsolutePath());
                return null;
            }
            if (localInstance.getId() == ModpackVersionManifest.INVALID_ID) {
                // TODO, not really sure how an instance can get into this state at the moment.
                //       but instead of generating a sentry event for the error message, we emit a warning and ignore the instance.
                LOGGER.warn("Instance install complete and missing 'version.json', Ignoring. {}", json.toAbsolutePath());
                return null;
            }
            return localInstance;
        } catch (Exception e) {
            LOGGER.error("Failed to load instance: {}", json.toAbsolutePath(), e);
            return null;
        }
    }
}
