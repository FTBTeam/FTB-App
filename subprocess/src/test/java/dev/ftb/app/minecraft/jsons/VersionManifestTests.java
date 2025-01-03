package dev.ftb.app.minecraft.jsons;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static dev.ftb.app.minecraft.jsons.VersionManifest.*;
import static org.junit.jupiter.api.Assertions.*;

public class VersionManifestTests {

    @BeforeAll
    public static void setup() {
        // Set debug mode to enabled in VersionManifest.
        // This will avoid caching the current OS, we can then set it for testing.
        System.setProperty("VersionManifest.debug", "true");
    }

    @Test
    public void testOsParse() {
        assertEquals(OS.WINDOWS, OS.parse("Windows"));

        assertEquals(OS.OSX, OS.parse("osx"));
        assertEquals(OS.OSX, OS.parse("mac"));

        assertEquals(OS.LINUX, OS.parse("linux"));

        assertEquals(OS.UNKNOWN, OS.parse("teapot"));
    }

    @Test
    public void testRules() {
        String prevName = System.getProperty("os.name");

        // Test Simple rule matches
        List<Rule> rules = new ArrayList<>();
        System.setProperty("os.name", "osx");
        rules.add(new Rule(Action.ALLOW, new OSRule(OS.OSX, null, null), null));
        assertTrue(Rule.apply(rules, Set.of()));

        // Test Combined rules.
        rules.clear();
        rules.add(new Rule(Action.ALLOW, null, null));
        rules.add(new Rule(Action.DISALLOW, new OSRule(OS.OSX, null, null), null));

        // Osx should result in false.
        System.setProperty("os.name", "osx");
        assertFalse(Rule.apply(rules, Set.of()));

        // Windows should result in true.
        System.setProperty("os.name", "Windows");
        assertTrue(Rule.apply(rules, Set.of()));

        System.setProperty("os.name", prevName);
    }
}
