//package net.creeperhost.creeperlauncher.instance.importer.providers;
//
//import com.google.gson.JsonElement;
//import net.covers1624.quack.platform.OperatingSystem;
//import net.creeperhost.creeperlauncher.instance.importer.meta.SimpleInstanceInfo;
//import net.creeperhost.creeperlauncher.util.FileUtils;
//import net.creeperhost.creeperlauncher.util.GsonUtils;
//import net.creeperhost.creeperlauncher.util.Result;
//import org.jetbrains.annotations.Nullable;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.LinkedList;
//import java.util.List;
//
//public class GdLauncherProvider implements InstanceProvider {
//    private static final Logger LOGGER = LoggerFactory.getLogger(GdLauncherProvider.class);
//    
//    @Override
//    public Result<Boolean, String> importInstance(String identifier) {
//        return Result.err("Not implemented");
//    }
//
//    @Override
//    public List<SimpleInstanceInfo> getAllInstances() {
//        var instancesPath = getDataLocation();
//        if (instancesPath == null || !Files.exists(instancesPath)) {
//            LOGGER.error("Failed to get instances dir");
//            return List.of();
//        }
//        
//        // Read the dir list
//        var instanceLocations = FileUtils.listDir(instancesPath)
//            .stream()
//            .filter(e -> Files.exists(e.resolve("config.json")))
//            .toList();
//        
//        List<SimpleInstanceInfo> simpleData = new LinkedList<>();
//        for (Path location : instanceLocations) {
//            try {
//                JsonElement instanceJson = GsonUtils.loadJson(location.resolve("config.json"), JsonElement.class);
//                var instance = instanceJson.getAsJsonObject();
//                var name = location.getFileName().toString();
//                
//                var loader = instance.get("loader").getAsJsonObject();
//                if (loader.isJsonNull()) {
//                    LOGGER.error("Failed to read instance.json");
//                    continue;
//                }
//                
//                var minecraftVersion = loader.get("mcVersion").getAsString();
//
//                simpleData.add(new SimpleInstanceInfo(name, location, minecraftVersion, null));
//            } catch (Exception e) {
//                LOGGER.error("Failed to read instance.json", e);
//            }
//        }
//        
//        return simpleData;
//    }
//
//    @Override
//    @Nullable
//    public SimpleInstanceInfo getInstance(String instanceName) {
//        return null;
//    }
//
//    @Override
//    @Nullable
//    public Path getDataLocation() {
//        return switch (OperatingSystem.current()) {
//            case WINDOWS -> throw new RuntimeException("Not implemented");
//            case LINUX, SOLARIS, FREEBSD -> throw new RuntimeException("Not implemented");
//            case MACOS -> Path.of("/Users/michael/Library/Application Support/gdlauncher_next/instances"); // gdlauncher_next could be wrong?
//            default -> null;
//        };
//    }
//
//    @Override
//    @Nullable
//    public JsonElement getDataFile(Path path) {
//        return null;
//    }
//}