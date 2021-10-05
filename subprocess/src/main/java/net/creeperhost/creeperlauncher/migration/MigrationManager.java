package net.creeperhost.creeperlauncher.migration;

import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.Settings;
import net.creeperhost.creeperlauncher.api.data.other.CloseModalData;
import net.creeperhost.creeperlauncher.api.data.other.YeetLauncherData;
import net.creeperhost.creeperlauncher.migration.migrators.DialogUtil;
import net.creeperhost.creeperlauncher.migration.migrators.LegacyMigrator;
import net.creeperhost.creeperlauncher.migration.migrators.V1To2;
import net.creeperhost.creeperlauncher.os.OS;
import net.creeperhost.creeperlauncher.util.ElapsedTimer;
import net.creeperhost.creeperlauncher.util.GsonUtils;
import net.creeperhost.creeperlauncher.util.LogsUploader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * TODO, some form of progress dialog.
 * Created by covers1624 on 13/1/21.
 */
public class MigrationManager {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Specifies the highest data format version the FTBApp is capable of reading.
     */
    private static final int CURRENT_DATA_FORMAT = 2;

    private static final Set<Class<? extends Migrator>> migrators = new HashSet<>();

    static {
        migrators.add(LegacyMigrator.class);
        migrators.add(V1To2.class);
    }

    private final Path formatJsonPath;
    private FormatJson formatJson;

    public MigrationManager() {
        formatJsonPath = Constants.getDataDir().resolve("._format.json");
        if (Files.exists(formatJsonPath)) {
            try {
                formatJson = GsonUtils.loadJson(formatJsonPath, FormatJson.class);
            } catch (IOException e) {
                LOGGER.fatal("Failed to read FormatJson. Assuming FormatJson does not exist.", e);
            }
        }
    }

    public int getDataFormat() {
        if (formatJson != null) {
            return formatJson.format;
        }

        Path settings = Constants.BIN_LOCATION.resolve("settings.json");
        if (Files.exists(settings)) return 1; // if no format json at this point, then we're at a 1

        OS os = OS.CURRENT;
        if (os != OS.LINUX) {
            Path oldDataDir = Constants.getDataDirOld();

            Path oldSettings = oldDataDir.resolve("bin/settings.json");
            if (Files.exists(oldSettings)) return -1; // legacy migration needs doing, hopefully eventually kill it
        }

        return CURRENT_DATA_FORMAT; // If we get here, then there's no existing data, so we don't need a migration
    }

    public void doMigrations() {
        int from = getDataFormat();
        if (from == CURRENT_DATA_FORMAT) {
            markAndSaveLatest();
            return;
        }
        ResourceBundle bundle = ResourceBundle.getBundle("MigrationMessages");

        if (from > CURRENT_DATA_FORMAT) {
            LOGGER.warn("Loaded newer data format from disk: {}, current: {}", from, CURRENT_DATA_FORMAT);
            boolean ret = DialogUtil.confirmDialog("Confirmation", bundle.getString("migration.newer_format"));
            if (!ret) {
                LOGGER.info("Exiting at user request.");
                Settings.webSocketAPI.sendMessage(new YeetLauncherData());
                System.exit(2);
                return;
            }
            LOGGER.warn("Ignoring warning at user request, forcibly reverting saved data format.");
            markAndSaveLatest();
            return;
        }

        try {
            LOGGER.info("Starting migration from {} to {}", from, CURRENT_DATA_FORMAT);
            ElapsedTimer startTimer = new ElapsedTimer();
            MigrationContext ctx = MigrationContext.buildContext(migrators, from);
            LOGGER.info("Built MigrationContext in {}", startTimer.elapsedStr());

            List<MigrationContext.MigratorState> migrators = ctx.getMigrators();

            if (migrators.isEmpty()) {
                LOGGER.info("No migrators will be run for this upgrade. Skipping..");
                markAndSaveLatest();
                return;
            }

            LOGGER.debug("Built migration list:");
            for (MigrationContext.MigratorState state : migrators) {
                Migrator.Properties properties = state.props;
                String name = state.migratorClass.getName();
                LOGGER.debug("{} to {} with {}", properties.from(), properties.to(), name);
            }

            boolean ret = DialogUtil.confirmDialog("Migration required", bundle.getString("migration.required"));
            if (!ret) {
                LOGGER.info("Exiting at user request.");
                Settings.webSocketAPI.sendMessage(new YeetLauncherData());
                System.exit(2);
                return;
            }

            for (MigrationContext.MigratorState state : migrators) {
                Migrator.Properties properties = state.props;
                String migratorName = state.migratorClass.getName();
                LOGGER.info("Executing migrator: {}", migratorName);
                ElapsedTimer migratorTimer = new ElapsedTimer();
                try {
                    state.migrator.operate(ctx);
                } catch (Throwable e) {
                    LOGGER.fatal("Fatal exception occurred whilst performing migration from {} to {} with {}.", properties.from(), properties.to(), migratorName, e);
                    captureMigrationError(bundle);
                }
                state.executed = true;
                LOGGER.info("Finished in {}", migratorTimer.elapsedStr());
            }
            LOGGER.info("Finished migration in {}", startTimer.elapsedStr());
            markAndSaveLatest();
        } catch (Throwable e) {
            LOGGER.fatal("Fatal exception occurred whilst performing migration.", e);
            captureMigrationError(bundle);
        }
    }

    private void markAndSaveLatest() {
        formatJson = new FormatJson(CURRENT_DATA_FORMAT);
        saveJson();
    }

    private void saveJson() {
        try {
            GsonUtils.saveJson(formatJsonPath, formatJson);
        } catch (IOException e) {
            LOGGER.fatal("Failed to save FormatJson.", e);
        }
    }

    private void captureMigrationError(ResourceBundle bundle) {
        String uploadCode = LogsUploader.uploadPaste(LogsUploader.getDebugLog());
        String logsHtml = uploadCode == null ? "Logs upload failed." : "<a href=\"https://pste.ch/" + uploadCode + "\">https://pste.ch/" + uploadCode + "</a>";
        DialogUtil.okDialog("Migration Error", bundle.getString("migration.error") +
                        "  Logs: " + logsHtml);
        Settings.webSocketAPI.sendMessage(new YeetLauncherData());
        System.exit(2);
    }

    private static class FormatJson {

        public String ___comment = "Stores what format the data is laid out in. DO NOT EDIT THIS FILE, THINGS WILL BREAK.";
        public int format;

        public FormatJson() {
        }

        public FormatJson(int format) {
            this();
            this.format = format;
        }
    }
}
