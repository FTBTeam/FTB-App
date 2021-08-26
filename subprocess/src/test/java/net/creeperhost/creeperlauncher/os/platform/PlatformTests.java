package net.creeperhost.creeperlauncher.os.platform;

import net.creeperhost.creeperlauncher.Constants;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by covers1624 on 8/3/21.
 */
public class PlatformTests {

    @Test
    public void testMacosPlatformLauncherArgs() {
        //Test that our Macos launcher process args are correct.
        MacosPlatform platform = new MacosPlatform();
        List<String> args = platform.prepareLauncherProcessArgs();
        assertEquals(5, args.size());
        assertEquals("/usr/bin/open", args.get(0));
        assertEquals(platform.getLauncherOpenPath().toAbsolutePath().toString(), args.get(1));
        assertEquals("--args", args.get(2));
        assertEquals("--workDir", args.get(3));
        assertEquals(Constants.BIN_LOCATION.toAbsolutePath().toString(), args.get(4));
    }

}
