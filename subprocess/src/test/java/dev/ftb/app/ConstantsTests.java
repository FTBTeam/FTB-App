package dev.ftb.app;

import dev.ftb.app.util.ModpackApiUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConstantsTests {

    @Test
    public void testGetModpackPrefix() {
        AppMain.isDevMode = false;
        // Test non private.
        assertEquals("https://api.feed-the-beast.com/v1/modpacks/modpack/", ModpackApiUtils.getModpacksEndpoint(false, (byte) 0));
        assertEquals("https://api.feed-the-beast.com/v1/modpacks/curseforge/", ModpackApiUtils.getModpacksEndpoint(false, (byte) 1));

        // Test private without key set
        assertEquals("https://api.feed-the-beast.com/v1/modpacks/modpack/", ModpackApiUtils.getModpacksEndpoint(true, (byte) 0));
        assertEquals("https://api.feed-the-beast.com/v1/modpacks/curseforge/", ModpackApiUtils.getModpacksEndpoint(true, (byte) 1));
        
        // Key has no effect on CurseForge.
        assertEquals("https://api.feed-the-beast.com/v1/modpacks/curseforge/", ModpackApiUtils.getModpacksEndpoint(true, (byte) 1));
    }
}
