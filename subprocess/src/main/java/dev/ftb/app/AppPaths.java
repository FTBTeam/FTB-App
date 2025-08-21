package dev.ftb.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

/**
 * Centralized class for managing application paths.
 */
public class AppPaths {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppPaths.class);
    
    private static final String DOT_FTBA = ".ftba"; // This is the inner data directory name.
    
    private final List<Path> paths = new LinkedList<>();
    
    private final Path workingDir;
    private final Path dataDir;
    private final Path binDir;
    private final Path storageDir;
    
    private final Path instancesDir;
    
    private final Path mcLibrariesDir;
    private final Path mcVersionsDir;
    private final Path mcAssetsDir;
    
    private final Path settingsFile;
    private final Path legacySettingsFile;
    private final Path storageFile;
    private final Path credentialsFiles;
    private final Path processMarkerFile;
    
    public AppPaths() {
        this.workingDir = Path.of(""); // This is where the app is run from and thus the working directory.
        
        this.dataDir = this.resolvePath(this.workingDir, DOT_FTBA);
        this.binDir = this.resolvePath(this.dataDir, "bin");
        this.storageDir = this.resolvePath(this.dataDir,"storage");
        
        this.mcLibrariesDir = this.resolvePath(this.binDir, "libraries");
        this.mcVersionsDir = this.resolvePath(this.binDir, "versions");
        this.mcAssetsDir = this.resolvePath(this.binDir, "assets");
        
        this.instancesDir = this.resolvePath(this.dataDir, "instances");
        
        this.legacySettingsFile = this.binDir.resolve("settings.json");
        this.settingsFile = this.storageDir.resolve("settings.json");
        this.storageFile = this.storageDir.resolve("storage.json");
        this.credentialsFiles = this.storageDir.resolve("credentials.encr");
        this.processMarkerFile = this.dataDir.resolve("app.pid");
        
        this.ensureDirectoriesExist();
        
        for (var path : this.paths) {
            LOGGER.debug("Path registered: {}, Absolute of: {}", path, path.toAbsolutePath());
        }
    }
    
    private void ensureDirectoriesExist() {
        for (Path path : this.paths) {
            try {
                if (Files.notExists(path)) {
                    Files.createDirectories(path);
                }
            } catch (Exception e) {
                // TODO: How can we handle this better?
                throw new RuntimeException("Failed to create directory: " + path, e);
            }
        }
    }
    
    private Path resolvePath(Path root, String subPath) {
        var resolvedPath = root.resolve(subPath);
        this.paths.add(resolvedPath);
        return resolvedPath;
    }

    public Path workingDir() {
        return workingDir;
    }

    public Path dataDir() {
        return dataDir;
    }

    public Path binDir() {
        return binDir;
    }

    public Path storageDir() {
        return storageDir;
    }

    public Path instancesDir() {
        return instancesDir;
    }

    public Path mcLibrariesDir() {
        return mcLibrariesDir;
    }
    
    public Path mcVersionsDir() {
        return mcVersionsDir;
    }
    
    public Path mcAssetsDir() {
        return mcAssetsDir;
    }

    public Path settingsFile() {
        return settingsFile;
    }

    public Path legacySettingsFile() {
        return legacySettingsFile;
    }

    public Path storageFile() {
        return storageFile;
    }

    public Path credentialsFiles() {
        return credentialsFiles;
    }
    
    public Path processMarkerFile() {
        return processMarkerFile;
    }
}
