package net.creeperhost.creeperlauncher.install.tasks.modloader;

import net.covers1624.quack.maven.MavenNotation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by covers1624 on 25/1/22.
 */
public class ForgeV1InstallTaskTests {

    @Test
    public void testComputeVersionName() {
        assertEquals(
                "1.12.2-forge1.12.2-14.23.5.2838",
                ForgeV1InstallTask.computeVersionName(MavenNotation.parse("net.minecraftforge:forge:1.12.2-14.23.5.2838"))
        );
    }
}
