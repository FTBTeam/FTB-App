package dev.ftb.app.pack;

import net.covers1624.quack.util.SneakyUtils;
import dev.ftb.app.Constants;
import dev.ftb.app.util.FileUtils;
import dev.ftb.app.util.ZipUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Creates a wrapper around a pack instance to handle fatal errors in a graceful way. We provide an 
 * instance and an action, this class then automatically creates a complete backup of that instance
 * that we're able to fall back onto in the event that something failed...
 * 
 * TODO: might want to check system disk space before backing up and restoring
 */
public class InstanceSnapshot {
    private static final Logger LOGGER = LogManager.getLogger();
    
    public static InstanceSnapshot create(Instance instance, Consumer<Instance> action) {
        return new InstanceSnapshot(instance, action);
    }
    
    private final Instance instance;
    private final Consumer<Instance> action;
    private final Instant createdAt;
    
    private final UUID snapshotIdentifier;
    private final Path snapshotLocation; 
    
    private Set<String> affectedRootFiles = new HashSet<>();
    
    private InstanceSnapshot(Instance instance, Consumer<Instance> action) {
        this.instance = instance;
        this.action = action;
        this.createdAt = Instant.now();
        
        this.snapshotIdentifier = UUID.randomUUID();
        this.snapshotLocation = Constants.getDataDir().resolve("snapshots/snapshot-%s-%s.zip".formatted(this.instance.getUuid(), this.snapshotIdentifier));
    }
    
    public InstanceSnapshot specifyEffectedFiles(Set<String> affectedRootFiles) {
        this.affectedRootFiles = affectedRootFiles;
        return this;
    }

    /**
     * Wraps the literal action with a snapshot creator and restorer whilst catching all errors
     * to ensure that the integrity of the instance is never compromised
     * 
     * @throws Throwable either an {@link InstanceSnapshotException} or any {@link Throwable}
     */
    public void run() throws Throwable {
        var snapshotResult = this.createSnapshot();
        if (!snapshotResult) {
            throw new InstanceSnapshotException("Unable to create snapshot of instance. It's unsafe to proceed past this point.");
        }
        
        try {
            this.action.accept(this.instance);
            
            // If successful, yeet the snapshot
            this.removeSnapshot();
        } catch (Throwable exception) {
            try {
                this.rollback();
                this.removeSnapshot();
            } catch (Throwable throwable) {
                // It's important we know when this has happened, so we don't delete the snapshot
                LOGGER.error("Unable to rollback to original snapshot data, please manually extract the contents of {} to {}", this.snapshotLocation, this.instance.getDir(), throwable);
                throw new InstanceSnapshotException("Unable to rollback to original data");
            }
            LOGGER.error("Fatal error caught when running the snapshots action...", exception);
            SneakyUtils.throwUnchecked(exception);
        }
    }

    /**
     * Creates the snapshot of the instance, if specific root paths are specified, we'll only back up
     * and restore those specific paths. 
     * 
     * @return if the snapshot was created successfully
     */
    private boolean createSnapshot() {
        Path snapshotPath = this.snapshotLocation.getParent();
        if (!Files.exists(snapshotPath)) {
            try {
                Files.createDirectories(snapshotPath);
            } catch (IOException e) {
                LOGGER.error("Unable to create `snapshots` path due to", e);
                return false;
            }
        }

        try {
            ZipUtils.createZipFromDirectory(this.instance.getDir(), this.snapshotLocation, this.affectedRootFiles);
            return true;
        } catch (IOException e) {
            LOGGER.error("Unable to create snapshot [{}] due to", this.snapshotIdentifier, e);
            return false;
        }
    }
    
    private void rollback() {
        LOGGER.info("Action failed, running fallback");

        try {
            LOGGER.info("Creating tmp directory for old files");
            Path instanceDir = this.instance.getDir();
            Path tmpLocation = instanceDir.resolve(".tmp-" + UUID.randomUUID());
            if (Files.notExists(tmpLocation)) {
                Files.createDirectories(tmpLocation);
            }

            LOGGER.info("Decompressing snapshot to tmp directory");
            ZipUtils.extractZip(this.snapshotLocation, tmpLocation);
            
            // If nothing was thrown, we know the decompression went well. Let's delete the 
            // instances folders and replace them with our new ones
            LOGGER.info("Removing existing instance files");
            if (this.affectedRootFiles.size() == 0) {
                try (var rootFiles = Files.walk(instanceDir, 1)) {
                    rootFiles.forEach(FileUtils::deletePath);
                }
            } else {
                this.affectedRootFiles.forEach(e -> FileUtils.deletePath(instanceDir.resolve(e)));
            }

            LOGGER.info("Moving tmp folders into the game directory");
            try (var tmpFiles = Files.walk(tmpLocation, 1)) {
                for (Path path : tmpFiles.toList()) {
                    if (path.equals(tmpLocation)) {
                        continue;
                    }
                    
                    org.apache.commons.io.FileUtils.moveDirectory(path.toFile(), instanceDir.resolve(path.getFileName().toString()).toFile());
                }
            }

            LOGGER.info("Removing {}", tmpLocation);
            org.apache.commons.io.FileUtils.deleteDirectory(tmpLocation.toFile());
            
            LOGGER.info("Removing {}", this.snapshotLocation);
            // Intentionally keep the snapshot if the try catch fails
            this.removeSnapshot();
        } catch (IOException e) {
            LOGGER.error("Fatal error on instance resetting. You can manually restore your instance by going to {} and unzipping the contents to {}", this.snapshotLocation, this.instance.getDir(), e);
        }
    }
    
    private void removeSnapshot() {
        try {
            Files.deleteIfExists(this.snapshotLocation);
        } catch (IOException ignored) {}
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
}
