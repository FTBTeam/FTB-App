package net.creeperhost.creeperlauncher;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.creeperhost.minetogether.lib.cloudsaves.CloudSaveManager;
import net.creeperhost.creeperlauncher.pack.LocalInstance;
import net.creeperhost.creeperlauncher.util.ElapsedTimer;
import net.creeperhost.creeperlauncher.util.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Instances
{
    private static final Logger LOGGER = LogManager.getLogger();

    private static Map<UUID, LocalInstance> instances = new HashMap<>();
    private static Map<UUID, JsonObject> cloudInstances = new HashMap<>();

    public static boolean addInstance(UUID uuid, LocalInstance instance)
    {
        Instances.instances.put(uuid, instance);
        return true;
    }

    public static LocalInstance getInstance(UUID uuid)
    {
        return Instances.instances.get(uuid);
    }

    public static List<String> listInstances()
    {
        return instances.keySet().stream().map(UUID::toString).collect(Collectors.toList());
    }

    //TODO, do these need to copy?
    public static List<LocalInstance> allInstances()
    {
        return new ArrayList<>(Instances.instances.values());
    }

    public static List<JsonObject> cloudInstances()
    {
        return new ArrayList<>(Instances.cloudInstances.values());
    }

    public static void refreshInstances() {
        ElapsedTimer totalTimer = new ElapsedTimer();
        Path instancesDir = Settings.getInstanceLocOr(Constants.INSTANCES_FOLDER_LOC);

        LOGGER.info("Reloading instances..");
        instances.clear();

        CompletableFuture<?> cloudFuture = reloadCloudInstances();

        if (!Files.exists(instancesDir)) {
            LOGGER.info("Instances directory missing, skipping..");
        } else {
            ElapsedTimer timer = new ElapsedTimer();
            List<LocalInstance> loadedInstances = FileUtils.listDir(instancesDir).stream()
                    .parallel()
                    .filter(e -> !e.getFileName().toString().startsWith("."))
                    .map(Instances::loadInstance)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            instances = loadedInstances.stream().collect(Collectors.toMap(LocalInstance::getUuid, Function.identity()));
            LOGGER.info("Loaded {} out of {} instances in {}.", instances.size(), loadedInstances.size(), timer.elapsedStr());
        }

        if (cloudFuture != null) {
            cloudFuture.join();
        }

        LOGGER.info("Finished instance reload in {}", totalTimer.elapsedStr());

    }


    public static CompletableFuture<?> reloadCloudInstances() {
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

    private static LocalInstance loadInstance(Path path) {
        Path json = path.resolve("instance.json");
        if (Files.notExists(json)) {
            LOGGER.error("Instance missing 'instance.json', Ignoring. {}", json.toAbsolutePath());
            return null;
        }
        try {
            LocalInstance localInstance = new LocalInstance(path);
            if (!localInstance.installComplete) {
                LOGGER.error("Instance install never completed, Ignoring. {}", json.toAbsolutePath());
                return null;
            }
            return localInstance;
        } catch(Exception e) {
            LOGGER.error("Instance has corrupted 'instance.json'. {}", json.toAbsolutePath());
            LOGGER.error(e);
            return null;
        }
    }

    private static HashMap<UUID, JsonObject> loadCloudInstances()
    {
        List<UUID> uuidList = CloudSaveManager.getPrefixes();
        HashMap<UUID, JsonObject> hashMap = new HashMap<>();

        for (UUID uuid : uuidList)
        {
            try
            {
                if(Instances.getInstance(uuid) == null)
                {
                    String jsonResp = CloudSaveManager.getFile(uuid.toString() + "/instance.json");
                    Gson gson = new Gson();
                    JsonObject object = gson.fromJson(jsonResp, JsonObject.class);
                    hashMap.put(uuid, object);
                }
            } catch (Exception e)
            {
                LOGGER.error("Invalid cloudsave found with UUID of {}", uuid);
            }

        }
        return hashMap;
    }
}
