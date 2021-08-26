package net.creeperhost.creeperlauncher.migration.migrators;

import com.google.gson.reflect.TypeToken;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.migration.MigrationContext;
import net.creeperhost.creeperlauncher.migration.Migrator;
import net.creeperhost.creeperlauncher.os.OS;
import net.creeperhost.creeperlauncher.util.FileUtils;
import net.creeperhost.creeperlauncher.util.GsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles moving FTBApp data from ~/.ftba on Windows and Mac.
 * <p>
 * Created by covers1624 on 13/1/21.
 */
@Migrator.Properties (from = -1, to = 1)
public class LegacyMigrator implements Migrator {

    private static final Logger LOGGER = LogManager.getLogger();

    private static final Type settingsToken = new TypeToken<HashMap<String, String>>() {}.getType();

    @Override
    public void operate(MigrationContext ctx) throws MigrationException {
    }
}
