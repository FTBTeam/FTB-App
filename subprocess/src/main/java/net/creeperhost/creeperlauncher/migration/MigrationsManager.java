package net.creeperhost.creeperlauncher.migration;

import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.migration.migrations.MigrateJVMDefaultsToInstances;
import net.creeperhost.creeperlauncher.util.GsonUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public enum MigrationsManager {
    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger(MigrationsManager.class);
    private static final Path MIGRATIONS_MEMORY = Constants.STORAGE_DIR.resolve("migrations.json");
    
    private static final List<Migration> migrations = List.of(
            new MigrateJVMDefaultsToInstances()
    );

    /**
     * Safely try and run migrations
     */
    public void runMigrations() {
        try {
            this._runMigrations();
        } catch (Exception exception) {
            LOGGER.error("Migrations process failed to run migrations", exception);   
        }
    }
    
    private void _runMigrations() {
        List<String> requiredMigrations = this.findRequiredMigrations();
        if (requiredMigrations == null) {
            throw new RuntimeException("Failed to load required migrations so aborting");
        }

        List<Migration> successfulMigrations = new ArrayList<>();
        for (Migration migration : migrations) {
            if (!requiredMigrations.contains(migration.id())) {
                continue;
            }
            
            if (!this.migrate(migration)) {
                LOGGER.error("Failed to run migration {}", migration.id());
                continue;
            }
            
            successfulMigrations.add(migration);
        }
        
        // Save the successful migrations to the memory file
        try {
            String json = GsonUtils.GSON.toJson(successfulMigrations.stream().map(Migration::id).toList());
            Files.writeString(MIGRATIONS_MEMORY, json);
        } catch (Exception exception) {
            LOGGER.error("Failed to save migrations to memory", exception);
        }
    }

    /**
     * Safely run a migration and cleanup if successful
     * @param migration Migration to run
     * @return True if successful
     */
    private boolean migrate(Migration migration) {
        try {
            var result = migration.migrate();
            if (result) {
                try {
                    migration.cleanup();
                } catch (Exception exception) {
                    LOGGER.error("Failed to cleanup migration {}", migration.id(), exception);
                }
            }
            
            return result;
        } catch (Exception exception) {
            LOGGER.error("Failed to run migration {}", migration.id(), exception);
            return false;
        }
    }

    /**
     * Find the required migrations to run based on the memory file
     * 
     * @return List of migration ids to run or null if failed to load
     */
    @Nullable
    private List<String> findRequiredMigrations() {
        if (Files.notExists(MIGRATIONS_MEMORY)) {
            return migrations.stream().map(Migration::id).toList();
        }

        // Load the string list from the migrations.json
        try {
            String json = Files.readString(MIGRATIONS_MEMORY);
            var migrationIds = GsonUtils.GSON.<List<String>>fromJson(json, List.class);
            
            // Diff the migration ids
            var allMigrations = migrations.stream().map(Migration::id).toList();
            
            // Compute list of missing migrations to run
            return allMigrations.stream()
                .filter(migrationId -> !migrationIds.contains(migrationId))
                .toList();
        } catch (Exception exception) {
            LOGGER.error("Failed to load migrations from memory", exception);
        }
        
        return null;
    }
}
