package dev.ftb.app.util;

import dev.ftb.app.util.mc.MinecraftVersion;
import dev.ftb.app.util.mc.MinecraftVersionPresets;
import dev.ftb.app.util.mc.MinecraftVersions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class MinecraftVersionsTest {
    @Test
    void parsesValidVersionToAvailableVersion() {
        List<String> tests = List.of(
           "1.0.0",
           "1.12.2",
           "1.16.5",
           "1.17.1", 
           "1.21.1"
        );
        
        for (String test : tests) {
            MinecraftVersion version = MinecraftVersions.INSTANCE.parse(test);
            assertNotNull(version);
        }
        
        // Now test all of it's own known values
        tests = MinecraftVersions.INSTANCE.versions().stream().map(MinecraftVersion::version).toList();
        
        // And again, test all of them.
        for (String test : tests) {
            MinecraftVersion version = MinecraftVersions.INSTANCE.parse(test);
            assertNotNull(version);
            assertEquals(test, version.version());
        }
    }
    
    @Test
    void parsesSnapshotsToTheirNearestVersions() {
        Map<String, MinecraftVersionPresets> tests = Map.of(
            "24w46a", MinecraftVersionPresets.MC1_21_4,
            "24w44a", MinecraftVersionPresets.MC1_21_4,
            "24w40a", MinecraftVersionPresets.MC1_21_2,
            "16w39b", MinecraftVersionPresets.MC1_11_0,
            "24w14potato", MinecraftVersionPresets.MC1_20_5
        );
        
        for (Map.Entry<String, MinecraftVersionPresets> test : tests.entrySet()) {
            MinecraftVersion version = MinecraftVersions.INSTANCE.parse(test.getKey());
            assertNotNull(version);
            assertEquals(test.getValue().getVersionData(), version);
        }
    }

    @Test
    void parsesReleaseClientToTheirFinalVersion() {
        Map<String, MinecraftVersionPresets> tests = Map.of(
            "1.16.5-rc1", MinecraftVersionPresets.MC1_16_5,
            "1.17.1-pre2", MinecraftVersionPresets.MC1_17_1,
            "1.21.1-rc6", MinecraftVersionPresets.MC1_21_1
        );
        
        for (Map.Entry<String, MinecraftVersionPresets> test : tests.entrySet()) {
            MinecraftVersion version = MinecraftVersions.INSTANCE.parse(test.getKey());
            assertNotNull(version);
            assertEquals(test.getValue().getVersionData(), version);
        }
    }

    @Test
    void providesLastMCVersionWhenGivenSnapshotOutOfRange() {
        var highestVersion = MinecraftVersionPresets.VALUES.getLast();
        
        var outOfRangeSnapshot = "99w99a"; // This would be year 99, week 99, which is not possible.
        var version = MinecraftVersions.INSTANCE.parse(outOfRangeSnapshot);

        assertEquals(highestVersion.getVersionData(), version);
    }
    
    @Test
    void providesNullWhenVersionOutOfRange() {
        var outOfRangeVersions = List.of(
            "0.0.0",
            "2.0.0",
            "1.999999999.0"
        );
        
        for (String version : outOfRangeVersions) {
            var parsed = MinecraftVersions.INSTANCE.parse(version);
            assertNull(parsed);
        }
    }
    
    @Test
    void parsesWhenGivenVersionWithLeadingZeroButMCDoesNotHaveIt() {
        var version = MinecraftVersions.INSTANCE.parse("1.0.0");
        assertNotNull(version);
        assertEquals("1.0", MinecraftVersionPresets.MC1_0_0.getVersionData().version());
    }
    
    @Test
    void parsesAllOfItsOwnVersionStringsBackToThemself() {
        Map<String, MinecraftVersion> versions = MinecraftVersions.INSTANCE.versions().stream().collect(Collectors.toMap(MinecraftVersion::version, Function.identity()));
        
        for (Map.Entry<String, MinecraftVersion> version : versions.entrySet()) {
            var parsed = MinecraftVersions.INSTANCE.parse(version.getKey());
            
            assertNotNull(parsed);
            assertEquals(version.getValue(), parsed);
        }
    }
}
