package net.creeperhost.creeperlauncher;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.creeperhost.creeperlauncher.data.modpack.ModpackVersionManifest;
import net.creeperhost.minetogether.lib.cloudsaves.CloudSaveManager;
import net.creeperhost.creeperlauncher.pack.Instance;
import net.creeperhost.creeperlauncher.util.ElapsedTimer;
import net.creeperhost.creeperlauncher.util.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Instances
{
    private static final Logger LOGGER = LogManager.getLogger();

    private static Map<UUID, Instance> instances = new HashMap<>();
    private static Map<UUID, JsonObject> cloudInstances = new HashMap<>();

    @Nullable
    public static Instance getInstance(UUID uuid) {
        return Instances.instances.get(uuid);
    }

    public static void addInstance(Instance instance) {
        instances.put(instance.getUuid(), instance);
    }

    public static Iterable<Instance> allInstances() {
        return Collections.unmodifiableCollection(Instances.instances.values());
    }

    public static Iterable<JsonObject> cloudInstances() {
        return Collections.unmodifiableCollection(Instances.cloudInstances.values());
    }

    public static void refreshInstances() {
        ElapsedTimer totalTimer = new ElapsedTimer();
        Path instancesDir = Settings.getInstancesDir();

        LOGGER.info("Reloading instances..");
        instances.clear();

        CompletableFuture<?> cloudFuture = reloadCloudInstances();

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
            instances = loadedInstances.stream().collect(Collectors.toMap(Instance::getUuid, Function.identity()));
            LOGGER.info("Loaded {} out of {} instances in {}.", instances.size(), loadedInstances.size(), timer.elapsedStr());
        }

        if (cloudFuture != null) {
            cloudFuture.join();
        }

        LOGGER.info("Finished instance reload in {}", totalTimer.elapsedStr());

    }


    public static CompletableFuture<?> reloadCloudInstances() {

        if (false) // FIXME, Disable loading of Cloud instances.
        if (StringUtils.isNotEmpty(Constants.S3_HOST) && StringUtils.isNotEmpty(Constants.S3_BUCKET) && StringUtils.isNotEmpty(Constants.S3_KEY) && StringUtils.isNotEmpty(Constants.S3_SECRET)) {
            return CompletableFuture.runAsync(() -> {
                ElapsedTimer timer = new ElapsedTimer();
                LOGGER.info("Loading cloud instances");
                cloudInstances = loadCloudInstances();
                LOGGER.info("Loaded {} cloud instances in {}.", cloudInstances.size(), timer.elapsedStr());
            });
        }
        LOGGER.info("Skipping Cloud instance reload.");
        return null;
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
        } catch(Exception e) {
            LOGGER.error("Failed to load instance: {}", json.toAbsolutePath(), e);
            return null;
        }
    }

    private static HashMap<UUID, JsonObject> loadCloudInstances() {
        List<UUID> uuidList = CloudSaveManager.getPrefixes();
        HashMap<UUID, JsonObject> hashMap = new HashMap<>();

        for (UUID uuid : uuidList) {
            try {
                if(Instances.getInstance(uuid) == null) {
                    String jsonResp = CloudSaveManager.getFile(uuid.toString() + "/instance.json");
                    Gson gson = new Gson();
                    JsonObject object = gson.fromJson(jsonResp, JsonObject.class);
                    hashMap.put(uuid, object);
                }
            } catch (Exception e) {
                LOGGER.error("Invalid cloudsave found with UUID of {}", uuid);
            }

        }
        return hashMap;
    }
}
