package net.creeperhost.creeperlauncher.api.handlers.instances;

import com.google.gson.annotations.JsonAdapter;
import net.covers1624.quack.gson.PathTypeAdapter;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.CreeperLauncher;
import net.creeperhost.creeperlauncher.Instances;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.BaseData;
import net.creeperhost.creeperlauncher.api.handlers.IMessageHandler;
import net.creeperhost.creeperlauncher.install.OperationProgressTracker;
import net.creeperhost.creeperlauncher.install.tasks.LocalCache;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * The meat of this class should likely be moved to a migration manager
 */
public class MoveInstancesHandler implements IMessageHandler<MoveInstancesHandler.Data> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MoveInstancesHandler.class);
    
    @Override
    public void handle(Data data) {
        var currentLocation = Path.of(Settings.settings.getOrDefault("instanceLocation", Constants.INSTANCES_FOLDER_LOC.toAbsolutePath().toString()));
        if (currentLocation.toString().isEmpty()) {
            // Something is borked, just assume the default location is the right one
            currentLocation = Constants.INSTANCES_FOLDER_LOC;
        }
        
        var newLocation = data.newLocation;
        
        LOGGER.info("Preparing to move instances from {} to {}", currentLocation, newLocation);
        
        // Very defensive checks
        if (newLocation == null) {
            LOGGER.warn("New location is null");
            Settings.webSocketAPI.sendMessage(new Reply(data, "New location can not be unset"));
            return;
        }
        
        if (newLocation.equals(currentLocation)) {
            LOGGER.warn("New location is the same as the current location");
            Settings.webSocketAPI.sendMessage(new Reply(data, "New location can not be the same as the current location"));
            return;
        }
        
        if (Files.notExists(newLocation)) {
            LOGGER.warn("New location {} does not exist", newLocation);
            Settings.webSocketAPI.sendMessage(new Reply(data, "New location does not exist"));
            return;
        }
        
        if (!Files.isDirectory(newLocation)) {
            LOGGER.warn("New location {} is not a directory", newLocation);
            Settings.webSocketAPI.sendMessage(new Reply(data, "New location is not a directory"));
            return;
        }
        
        // Ensure the new location is not a subdirectory of the current location
        if (newLocation.startsWith(currentLocation)) {
            LOGGER.warn("New location {} is a subdirectory of the current location {}", newLocation, currentLocation);
            Settings.webSocketAPI.sendMessage(new Reply(data, "New location is a subdirectory of the current location"));
            return;
        }
        
        // Also, don't allow the data folder
        if (newLocation.equals(Constants.getDataDir())) {
            LOGGER.warn("New location {} is the data folder", newLocation);
            Settings.webSocketAPI.sendMessage(new Reply(data, "New location can not be the data folder"));
            return;
        }
        
        if (!Files.isWritable(newLocation)) {
            LOGGER.warn("New location {} is not writable", newLocation);
            Settings.webSocketAPI.sendMessage(new Reply(data, "New location is not writable"));
            return;
        }
        
        // Finally ensure the app can write to the new location
        // This is super overkill, but it's better to be safe than sorry
        if (!net.creeperhost.creeperlauncher.util.FileUtils.pathWritableByApp(newLocation)) {
            LOGGER.warn("New location {} is not writable by the app", newLocation);
            Settings.webSocketAPI.sendMessage(new Reply(data, "New location is not writable by the FTB App"));
            return;
        }

        LOGGER.info("New location {} is valid, move started", newLocation);
        
        // We're good to go
        this.moveInstances(data, currentLocation, newLocation);
        
        // Send a reply to say we're processing
        Settings.webSocketAPI.sendMessage(new Reply(data, "processing", ""));
    }

    /**
     * TODO: markers should allow us to resume a move if the app crashes or something
     */
    private void moveInstances(Data data, Path currentLocation, Path newLocation) {
        var tracker = new OperationProgressTracker("instance-move", Map.of(
            "currentLocation", currentLocation.toString(),
            "newLocation", newLocation.toString()
        ));
        
        CompletableFuture.runAsync(() -> {
            LOGGER.info("Progressing: moving instances from {} to {}", currentLocation, newLocation);
            tracker.nextStage(MoveStage.COPYING);
            
            // Try and place a test file in the new location
            try {
                Files.createFile(newLocation.resolve(".test"));
            } catch (Throwable t) {
                throw new RuntimeException("Failed to create test file in new location", t);
            }
            
            // Remove the test file, We don't care if this fails
            try {
                Files.delete(newLocation.resolve(".test"));
            } catch (Throwable ignored) {}
            
            if (Files.exists(currentLocation.resolve(".moving"))) {
                LOGGER.warn("Old location {} is still being moved", currentLocation);
                // We're in the middle of a move, so we need to wait for the old location to be deleted
                throw new RuntimeException("Old location is still being moved");
            }
            
            // Place a marker file in the old location
            try {
                Files.createFile(currentLocation.resolve(".moving"));
            } catch (Throwable t) {
                // I'm on the edge about this being a fatal error, but I think it's fine
                throw new RuntimeException("Failed to create marker file in old location", t);
            }
            
            // Very carefully move the instances
            // Move the directories by copying them, then deleting the old ones if the copy succeeds
            int requiredSuccesses = 0;
            List<Path> successfulFiles = new ArrayList<>();
            
            try (var files = Files.list(currentLocation)) {
                if (files == null) throw new RuntimeException("Failed to list files in old location");
                
                var fileList = files.toList();
                
                for (var path : fileList) {
                    if (Files.isDirectory(path)) {
                        requiredSuccesses ++;
                        try {
                            LOGGER.info("Moving instance directory {} to {}", path.getFileName(), newLocation.resolve(path.getFileName()));
                            FileUtils.copyDirectory(path.toFile(), newLocation.resolve(path.getFileName()).toFile());
                            successfulFiles.add(path);
                        } catch (Throwable t) {
                            throw new RuntimeException("Failed to move instance directory " + path.getFileName(), t);
                        }
                    }
                }
            } catch (Throwable t) {
                throw new RuntimeException("Failed to move instances", t);
            }

            tracker.nextStage(MoveStage.VALIDATING);
            // Overkill, but just in case
            if (successfulFiles.size() != requiredSuccesses) {
                throw new RuntimeException("Failed to move instances, not all instances were moved... Moved " + successfulFiles.size() + " out of " + requiredSuccesses);
            }

            LOGGER.info("Successfully moved instances from {} to {}", currentLocation, newLocation);
            LOGGER.info("Removing old instance directories");

            tracker.nextStage(MoveStage.REMOVING_OLD);
            // Delete the old directories
            List<Path> failedRemovals = new ArrayList<>();
            for (var path : successfulFiles) {
                if (!Files.isDirectory(path)) {
                    continue;
                }
                
                try {
                    FileUtils.deleteDirectory(path.toFile());
                } catch (Throwable t) {
                    LOGGER.warn("Failed to remove instance directory {}", path.getFileName(), t);
                    failedRemovals.add(path);
                }
            }
            
            // Remove the marker file
            try {
                Files.delete(currentLocation.resolve(".moving"));
            } catch (Throwable ignored) {}
            
            // Not really a fatal error, but we should probably let the user know
            if (!failedRemovals.isEmpty()) {
                LOGGER.warn("Failed to remove some instance directories, please remove them manually. {}", failedRemovals);
                Settings.webSocketAPI.sendMessage(new Reply(data, "Failed to remove some instance directories, please remove them manually. " + failedRemovals));
            }
        }).whenComplete((aVoid, throwable) -> {
            tracker.finished();
            
            if (throwable != null) {
                LOGGER.error("Failed to move instances from {} to {}", currentLocation, newLocation, throwable);
                Settings.webSocketAPI.sendMessage(new Reply(data, throwable.getMessage()));
            } else {
                LOGGER.info("Successfully completed moving instances from {} to {}", currentLocation, newLocation);
                
                Settings.settings.remove("instanceLocation");
                Settings.settings.put("instanceLocation", newLocation.toAbsolutePath().toString());
                Settings.saveSettings();
                CreeperLauncher.localCache = new LocalCache(newLocation.resolve(".localCache"));
                Instances.refreshInstances();
                Path oldCache = currentLocation.resolve(".localCache");
                oldCache.toFile().deleteOnExit();
                Settings.webSocketAPI.sendMessage(new Reply(data, "success", ""));
            }
        });
    }

    public enum MoveStage implements OperationProgressTracker.Stage {
        COPYING,
        VALIDATING,
        REMOVING_OLD,        
    }

    public static class Data extends BaseData {
        @JsonAdapter(PathTypeAdapter.class)
        public Path newLocation;
    }
    
    public static class Reply extends Data {
        public String state;
        public String error;
        
        public Reply(Data data, String state, String error) {
            super();
            this.type = "moveInstancesReply";
            this.requestId = data.requestId;
            this.newLocation = data.newLocation;
            this.state = state;
            this.error = error;
        }

        public Reply(Data data, String error) {
            this(data, "error", error);
        }
        
        public Reply(Data data) {
            this(data, "success", "");
        }
    }
}
