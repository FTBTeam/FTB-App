package dev.ftb.app;

import dev.ftb.app.util.ModpacksChUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConstantsTests {

    @Test
    public void testGetModpackPrefix() {
        AppMain.isDevMode = false;
        // Test non private.
        assertEquals("https://api.feed-the-beast.com/v1/modpacks/public/modpack/", ModpacksChUtils.getModpacksEndpoint(false, (byte) 0));
        assertEquals("https://api.feed-the-beast.com/v1/modpacks/public/curseforge/", ModpacksChUtils.getModpacksEndpoint(false, (byte) 1));

        // Test private without key set
        assertEquals("https://api.feed-the-beast.com/v1/modpacks/public/modpack/", ModpacksChUtils.getModpacksEndpoint(true, (byte) 0));
        assertEquals("https://api.feed-the-beast.com/v1/modpacks/public/curseforge/", ModpacksChUtils.getModpacksEndpoint(true, (byte) 1));

        // Test with key set
        ModpacksChUtils.API_TOKEN = "potatoes";
        assertEquals("https://api.feed-the-beast.com/v1/modpacks/potatoes/modpack/", ModpacksChUtils.getModpacksEndpoint(true, (byte) 0));
        // Key has no effect on CurseForge.
        assertEquals("https://api.feed-the-beast.com/v1/modpacks/public/curseforge/", ModpacksChUtils.getModpacksEndpoint(true, (byte) 1));

        // Unknown ID's should default to type 0
        assertEquals("https://api.feed-the-beast.com/v1/modpacks/potatoes/unknown/", ModpacksChUtils.getModpacksEndpoint(true, (byte) 69));
    }
}
