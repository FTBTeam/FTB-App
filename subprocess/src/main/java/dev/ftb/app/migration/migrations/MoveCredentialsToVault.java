package dev.ftb.app.migration.migrations;

import dev.ftb.app.migration.Migration;
import dev.ftb.app.services.Services;
import dev.ftb.app.services.storage.VaultException;
import dev.ftb.app.storage.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class MoveCredentialsToVault implements Migration {
    private static final Logger LOGGER = LoggerFactory.getLogger(MoveCredentialsToVault.class);
    
    private static final Map<String, String> KEY_MAPPING = Map.of(
            "ftbAccount", "ftbAccount",
            "proxyPassword", Settings.PROXY_PASSWORD_KEY
    );
    
    @Override
    public String id() {
        return "move_credentials_to_vault";
    }

    @Override
    public boolean migrate(Services services) {
        var legacyStore = services.storage().legacyCredentialStorage();

        Map<String, String> entries = new HashMap<>();
        for (var entry : KEY_MAPPING.entrySet()) {
            var value = legacyStore.get(entry.getKey());
            if (value != null) {
                entries.put(entry.getValue(), value);
            }
        }

        if (entries.isEmpty()) {
            return true;
        }

        try {
            // One write, so either everything is moved or nothing is
            services.storage().vault().putAll(entries);
            return true;
        } catch (VaultException e) {
            // Failing keeps the legacy file (cleanup only runs on success) and retries the migration next launch
            LOGGER.error("Failed to move legacy credentials to the vault, will retry next launch", e);
            return false;
        }
    }

    @Override
    public void cleanup(Services services) {
        // If it worked, delete the old file.
        try {
            Path path = services.paths().credentialsFiles();
            Files.deleteIfExists(path);
        } catch (Exception e) {
            LOGGER.error("Failed to delete legacy credential storage file", e);
        }
    }
}
