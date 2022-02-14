package net.creeperhost.creeperlauncher.util;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by covers1624 on 3/1/22.
 */
public class MiscUtilsTests {

    @Test
    public void testSplitCommand() {
        List<String> expected = ImmutableList.of("hello", "world", "i have spaces :D", "woo", "AAAAA", "asd adads sadasdasdasd", "doisgdf");
        String str = expected.stream().map(e -> e.contains(" ") ? "\"" + e + "\"" : e).collect(Collectors.joining(" "));
        assertEquals(expected, MiscUtils.splitCommand(str));

        String cmd = "    a   b   c d e   \"f g\" h  i";
        assertEquals(ImmutableList.of("a", "b", "c", "d", "e", "f g", "h", "i"), MiscUtils.splitCommand(cmd));
    }

    @Test
    public void testEncodeUrlPath() {
        assertEquals(
                "https://edge.forgecdn.net/files/2229/132/BiblioWoods%5BNatura%5D%5Bv1.5%5D.jar",
                MiscUtils.encodeURL("https://edge.forgecdn.net/files/2229/132/BiblioWoods[Natura][v1.5].jar")
        );
        assertEquals(
                "https://dist.modpacks.ch/modpacks/0/FTB%20Infinity%20Evolved%201.7-3.1.0/mods/PortalGunSounds.pak",
                MiscUtils.encodeURL("https://dist.modpacks.ch/modpacks/0/FTB Infinity Evolved 1.7-3.1.0/mods/PortalGunSounds.pak")
        );
    }
}
