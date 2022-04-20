package net.creeperhost.creeperlauncher.install.tasks;

import net.covers1624.quack.util.MultiHasher.HashFunc;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Created by covers1624 on 14/4/22.
 */
public class NewDownloadTaskTests {

    static {
        System.setProperty("DownloadTask.debug", "true");
    }

    @Test
    public void testCompanionHashes() throws Throwable {
        Path tempDir = Files.createTempDirectory("dlTask");
        tempDir.toFile().deleteOnExit();

        NewDownloadTask task = NewDownloadTask.builder()
                .url("https://repo1.maven.org/maven2/com/google/guava/guava/maven-metadata.xml")
                .dest(tempDir.resolve("maven-metadata.xml"))
                .tryCompanionHashes()
                .build();
        task.execute(null, null);
        assertNotNull(task.getValidation().expectedHashes.get(HashFunc.SHA1));
    }
}
