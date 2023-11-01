package net.creeperhost.creeperlauncher;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by covers1624 on 19/11/21.
 */
public class ConstantsTests {

    @Test
    public void testGetCreeperHostModpackPrefix() {
        CreeperLauncher.isDevMode = false;
        // Test non private.
        assertEquals("https://api.modpacks.ch/public/modpack/", Constants.getModpacksEndpoint(false, (byte) 0));
        assertEquals("https://api.modpacks.ch/public/curseforge/", Constants.getModpacksEndpoint(false, (byte) 1));

        // Test private without key set
        assertEquals("https://api.modpacks.ch/public/modpack/", Constants.getModpacksEndpoint(true, (byte) 0));
        assertEquals("https://api.modpacks.ch/public/curseforge/", Constants.getModpacksEndpoint(true, (byte) 1));

        // Test with key set
        Constants.KEY = "potatoes";
        assertEquals("https://api.modpacks.ch/potatoes/modpack/", Constants.getModpacksEndpoint(true, (byte) 0));
        // Key has no effect on CurseForge.
        assertEquals("https://api.modpacks.ch/public/curseforge/", Constants.getModpacksEndpoint(true, (byte) 1));

        // Unknown ID's should default to type 0
        assertEquals("https://api.modpacks.ch/potatoes/modpack/", Constants.getModpacksEndpoint(true, (byte) 69));
    }
}
