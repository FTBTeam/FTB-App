package dev.ftb.app.util;

import dev.ftb.app.AppMain;
import dev.ftb.app.os.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Utility class for managing Windows GPU preferences for Java executables.
 * This configures the Windows Graphics settings to run javaw.exe on high-performance GPU (dGPU)
 * instead of the integrated GPU (iGPU) to prevent performance issues and crashes.
 */
public class WindowsGpuPreferences {
    private static final Logger LOGGER = LoggerFactory.getLogger(WindowsGpuPreferences.class);
    
    private static final String REGISTRY_KEY = "HKEY_CURRENT_USER\\SOFTWARE\\Microsoft\\DirectX\\UserGpuPreferences";
    private static final String GPU_PREFERENCE_VALUE = "GpuPreference=2;"; // 0=auto, 1=power saving, 2=high performance
    
    /**
     * Configures all javaw.exe executables in the runtime folder to use high-performance GPU.
     * Only works on Windows. Does nothing on other platforms.
     * 
     * @return true if successful or not on Windows, false if an error occurred
     */
    public static boolean configureAllJavaRuntimes() {
        if (OS.CURRENT != OS.WIN) {
            LOGGER.debug("Not on Windows, skipping GPU preference configuration");
            return true;
        }
        
        List<Path> javawPaths = findAllJavawExecutables();
        if (javawPaths.isEmpty()) {
            LOGGER.info("No javaw.exe executables found in runtime folder");
            return true;
        }
        
        LOGGER.info("Found {} javaw.exe executables to configure for high-performance GPU", javawPaths.size());
        
        boolean success = true;
        for (Path javawPath : javawPaths) {
            if (!setGpuPreference(javawPath)) {
                LOGGER.error("Failed to set GPU preference for: {}", javawPath);
                success = false;
            }
        }
        
        return success;
    }
    
    /**
     * Configures a specific javaw.exe to use high-performance GPU.
     * 
     * @param javawPath Path to javaw.exe
     * @return true if successful, false otherwise
     */
    public static boolean configureJavaRuntime(Path javawPath) {
        if (OS.CURRENT != OS.WIN) {
            LOGGER.debug("Not on Windows, skipping GPU preference configuration");
            return true;
        }
        
        if (!Files.exists(javawPath)) {
            LOGGER.warn("javaw.exe does not exist: {}", javawPath);
            return false;
        }
        
        return setGpuPreference(javawPath);
    }
    
    /**
     * Finds all javaw.exe executables in the runtime folder.
     * 
     * @return List of paths to javaw.exe executables
     */
    private static List<Path> findAllJavawExecutables() {
        List<Path> javawPaths = new ArrayList<>();
        Path runtimeDir = AppMain.paths().binDir().resolve("runtime");
        
        if (!Files.exists(runtimeDir)) {
            LOGGER.debug("Runtime directory does not exist: {}", runtimeDir);
            return javawPaths;
        }
        
        try (Stream<Path> paths = Files.walk(runtimeDir)) {
            paths.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().equalsIgnoreCase("javaw.exe"))
                .forEach(javawPaths::add);
        } catch (IOException e) {
            LOGGER.error("Failed to search for javaw.exe in runtime directory", e);
        }
        
        return javawPaths;
    }
    
    /**
     * Sets the GPU preference for a specific executable using Windows registry.
     * 
     * @param executablePath Path to the executable
     * @return true if successful, false otherwise
     */
    private static boolean setGpuPreference(Path executablePath) {
        String absolutePath = executablePath.toAbsolutePath().toString();
        
        try {
            // Use reg.exe to add the registry value
            // REG ADD key /v valueName /t REG_SZ /d data /f
            ProcessBuilder pb = new ProcessBuilder(
                "reg", "add", REGISTRY_KEY,
                "/v", absolutePath,
                "/t", "REG_SZ",
                "/d", GPU_PREFERENCE_VALUE,
                "/f" // Force overwrite if exists
            );
            
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                LOGGER.info("Successfully set GPU preference for: {}", absolutePath);
                return true;
            } else {
                // Read error output
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String errorOutput = reader.lines().reduce("", (a, b) -> a + "\n" + b);
                    LOGGER.error("Failed to set GPU preference for: {}. Exit code: {}. Error: {}", 
                        absolutePath, exitCode, errorOutput);
                }
                return false;
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Failed to set GPU preference for: " + absolutePath, e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }
    
    /**
     * Removes GPU preference for a specific executable.
     * This can be useful for cleanup or if the user wants to revert the setting.
     * 
     * @param executablePath Path to the executable
     * @return true if successful, false otherwise
     */
    public static boolean removeGpuPreference(Path executablePath) {
        if (OS.CURRENT != OS.WIN) {
            return true;
        }
        
        String absolutePath = executablePath.toAbsolutePath().toString();
        
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "reg", "delete", REGISTRY_KEY,
                "/v", absolutePath,
                "/f"
            );
            
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                LOGGER.info("Successfully removed GPU preference for: {}", absolutePath);
                return true;
            } else {
                LOGGER.debug("Failed to remove GPU preference (may not exist): {}", absolutePath);
                return false;
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Failed to remove GPU preference for: " + absolutePath, e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }
}
