package net.creeperhost.creeperlauncher.migration.migrators;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import net.covers1624.quack.util.HashUtils;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.install.tasks.LocalCache;
import net.creeperhost.creeperlauncher.migration.MigrationContext;
import net.creeperhost.creeperlauncher.migration.Migrator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Created by covers1624 on 22/1/21.
 */
@SuppressWarnings ("UnstableApiUsage")
@Migrator.Properties (from = 1, to = 2)
public class V1To2 implements Migrator {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final HashFunction SHA1 = Hashing.sha1();

    @Override
    public void operate(MigrationContext ctx) {
        LOGGER.info("Starting migration of local cache.");
        Settings.loadSettings();
        Path cacheLocation = Settings.getInstanceLocOr(Constants.INSTANCES_FOLDER_LOC).resolve(".localCache");
        Set<Path> files = new HashSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(cacheLocation)) {
            for (Path file : stream) {
                String name = file.getFileName().toString();
                if (Files.isRegularFile(file) && !name.endsWith("index.json")) {
                    try {
                        UUID.fromString(name);//Try parse name.
                        files.add(file);
                    } catch (Exception e) {
                        LOGGER.warn("Deleting invalid file from cache directory: {}", file);
                        try {
                            Files.delete(file);
                        } catch (IOException ignored) {
                        }
                    }
                }
            }
        } catch (IOException ignored) {
        }
        if (files.isEmpty()) {
            LOGGER.info("No files for migration found.");
            return;
        }
        LOGGER.info("Identified {} files for migration.", files.size());

        try (LocalCache cache = new LocalCache(cacheLocation)) {
            cache.disableAutoSave();
            files.parallelStream().forEach(file -> {
                boolean error;
                try {
                    HashCode hash = HashUtils.hash(SHA1, file);
                    error = !cache.ingest(file, hash);
                } catch (IOException e) {
                    error = true;
                    LOGGER.warn("IO error occurred hashing file.", e);
                }
                if (error) {
                    try {
                        Files.deleteIfExists(file);
                    } catch (IOException e) {
                        LOGGER.error("Failed to delete file.", e);
                    }
                }
            });
        }
        LOGGER.info("Finished migrating files.");
    }
}
