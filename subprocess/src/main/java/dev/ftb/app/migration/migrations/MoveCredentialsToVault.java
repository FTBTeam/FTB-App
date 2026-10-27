package dev.ftb.app.migration.migrations;

import dev.ftb.app.migration.Migration;
import dev.ftb.app.services.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class MoveCredentialsToVault implements Migration {
    private static final Logger LOGGER = LoggerFactory.getLogger(MoveCredentialsToVault.class);
    
    private static final Map<String, String> KEY_MAPPING = Map.of(
            "ftbAccount", "ftbAccount",
            "proxyPassword", "proxy.password"
    );
    
    @Override
    public String id() {
        return "move_credentials_to_vault";
    }

    @Override
    public boolean migrate(Services services) {
        var legacyStore = services.storage().legacyCredentialStorage();
        var vaultStore = services.storage().vault();
        
        for (var entry : KEY_MAPPING.entrySet()) {
            var legacyKey = entry.getKey();
            var vaultKey = entry.getValue();
            
            var value = legacyStore.get(legacyKey);
            if (value != null) {
                try {
                    vaultStore.put(vaultKey, value);
                } catch (Exception e) {
                    // Log the error and continue with the next key
                    LOGGER.error("Failed to migrate key '{}' to vault as '{}'", legacyKey, vaultKey, e);
                }
            }
        }
        
        return true;
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
