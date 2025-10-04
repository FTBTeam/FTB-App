package dev.ftb.app.util;

import dev.ftb.app.os.OS;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for WindowsGpuPreferences utility.
 * Note: Actual registry operations can only be tested on Windows.
 */
public class WindowsGpuPreferencesTest {

    @Test
    public void testConfigureAllJavaRuntimesOnNonWindows() {
        // On non-Windows systems, this should always return true without doing anything
        if (OS.CURRENT != OS.WIN) {
            boolean result = WindowsGpuPreferences.configureAllJavaRuntimes();
            assertTrue(result, "Should succeed on non-Windows systems");
        }
    }

    @Test
    public void testConfigureJavaRuntimeWithNonExistentPath() {
        // Test with a path that doesn't exist
        Path nonExistentPath = Path.of("/nonexistent/javaw.exe");
        
        if (OS.CURRENT == OS.WIN) {
            boolean result = WindowsGpuPreferences.configureJavaRuntime(nonExistentPath);
            assertFalse(result, "Should fail for non-existent path on Windows");
        } else {
            boolean result = WindowsGpuPreferences.configureJavaRuntime(nonExistentPath);
            assertTrue(result, "Should succeed on non-Windows systems even with non-existent path");
        }
    }

    @Test
    public void testRemoveGpuPreferenceOnNonWindows() {
        // On non-Windows systems, this should always return true
        if (OS.CURRENT != OS.WIN) {
            Path dummyPath = Path.of("/dummy/javaw.exe");
            boolean result = WindowsGpuPreferences.removeGpuPreference(dummyPath);
            assertTrue(result, "Should succeed on non-Windows systems");
        }
    }
}
