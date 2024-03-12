package net.creeperhost.creeperlauncher;

import net.creeperhost.creeperlauncher.util.ModpacksChUtils;
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
        assertEquals("https://api.modpacks.ch/public/modpack/", ModpacksChUtils.getModpacksEndpoint(false, (byte) 0));
        assertEquals("https://api.modpacks.ch/public/curseforge/", ModpacksChUtils.getModpacksEndpoint(false, (byte) 1));

        // Test private without key set
        assertEquals("https://api.modpacks.ch/public/modpack/", ModpacksChUtils.getModpacksEndpoint(true, (byte) 0));
        assertEquals("https://api.modpacks.ch/public/curseforge/", ModpacksChUtils.getModpacksEndpoint(true, (byte) 1));

        // Test with key set
        ModpacksChUtils.API_TOKEN = "potatoes";
        assertEquals("https://api.modpacks.ch/potatoes/modpack/", ModpacksChUtils.getModpacksEndpoint(true, (byte) 0));
        // Key has no effect on CurseForge.
        assertEquals("https://api.modpacks.ch/public/curseforge/", ModpacksChUtils.getModpacksEndpoint(true, (byte) 1));

        // Unknown ID's should default to type 0
        assertEquals("https://api.modpacks.ch/potatoes/unknown/", ModpacksChUtils.getModpacksEndpoint(true, (byte) 69));
    }
}
