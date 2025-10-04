package dev.ftb.app.migration.migrations;

import dev.ftb.app.migration.Migration;
import dev.ftb.app.os.OS;
import dev.ftb.app.storage.settings.Settings;
import dev.ftb.app.util.WindowsGpuPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Migration to configure Windows GPU preferences for all existing Java installations.
 * This sets all javaw.exe executables to run on the high-performance GPU (dGPU)
 * to prevent performance issues and crashes on systems with both iGPU and dGPU.
 */
public class MigrateSetupWindowsGpuPreferences implements Migration {
    private final Logger logger = LoggerFactory.getLogger(MigrateSetupWindowsGpuPreferences.class);
    
    @Override
    public String id() {
        return "migrate_setup_windows_gpu_preferences";
    }

    @Override
    public boolean migrate() {
        // Only run on Windows
        if (OS.CURRENT != OS.WIN) {
            logger.debug("Not on Windows, skipping GPU preference migration");
            return true;
        }
        
        // Check if the user has enabled this feature (default to true for new users)
        if (!Settings.getSettings().workaround().autoConfigureWindowsGpu()) {
            logger.info("Windows GPU preference configuration is disabled in settings");
            return true;
        }
        
        logger.info("Configuring Windows GPU preferences for existing Java installations");
        
        boolean success = WindowsGpuPreferences.configureAllJavaRuntimes();
        
        if (success) {
            logger.info("Successfully configured GPU preferences for all Java runtimes");
        } else {
            logger.warn("Some GPU preferences failed to configure, but continuing anyway");
        }
        
        // Return true even if some preferences failed - this is not critical to app functionality
        return true;
    }
}
