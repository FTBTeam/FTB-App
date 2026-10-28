package dev.ftb.app.storage.settings;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.ftb.app.AppPaths;
import dev.ftb.app.services.AppStorage;
import dev.ftb.app.services.storage.VaultException;
import dev.ftb.app.util.JsonStore;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.function.Consumer;

public class Settings {
    private static final Logger LOGGER = LoggerFactory.getLogger(Settings.class);
    private static final Type LEGACY_SETTINGS_TOKEN = new TypeToken<HashMap<String, String>>(){}.getType();

    private static final Gson GSON = new Gson().newBuilder()
        .setPrettyPrinting()
        .create();

    public static final String PROXY_PASSWORD_KEY = "proxy.password";

    private final AppPaths appPaths;
    private final AppStorage storage;
    private final JsonStore<SettingsData> store;

    public Settings(AppPaths appPaths, AppStorage storage) {
        this.appPaths = appPaths;
        this.storage = storage;

        this.store = new JsonStore<>(
            appPaths.settingsFile(),
            GSON,
            SettingsData.class,
            () -> SettingsData.createDefault(this.appPaths)
        );

        attemptMigration();
        load();
    }

    public SettingsData get() {
        return this.store.get();
    }

    /// Mutates the settings and saves them.
    public void update(Consumer<SettingsData> mutator) throws IOException {
        this.store.update(mutator);
    }

    /// Replaces all the settings, as sent by the frontend, and saves them.
    public void replace(SettingsData data) throws IOException, VaultException {
        var proxy = data.proxy();
        
        // Older settings files kept the proxy password in plain text. Move it to the vault if it's there.
        if (proxy != null && StringUtils.isNotEmpty(proxy.password())) {
            this.storage.vault().put(PROXY_PASSWORD_KEY, proxy.password());
            proxy.setPassword("");
        } else if (usesProxyAuth(get()) && !usesProxyAuth(data)) {
            this.storage.vault().remove(PROXY_PASSWORD_KEY);
        }

        this.store.set(data);
    }

    /// Called after every change has been saved.
    public void onChange(Consumer<SettingsData> listener) {
        this.store.onChange(listener);
    }

    /// @return the proxy password, or `null` if the proxy doesn't use one or the vault can't be read
    @Nullable
    public String proxyPassword() {
        if (!usesProxyAuth(get())) {
            return null;
        }

        try {
            return this.storage.vault().get(PROXY_PASSWORD_KEY);
        } catch (VaultException e) {
            LOGGER.error("Failed to retrieve proxy password from vault", e);
            return null;
        }
    }

    private void load() {
        try {
            this.store.load();
        } catch (IOException e) {
            LOGGER.error("Failed to load settings file. Falling back to default.", e);
            return;
        }

        // Something (likely path parsing) has left this pointing at the app directory before, which would install
        // instances into the app root. Not saved, the next settings change writes it back.
        Path instanceLocation = get().instanceLocation();
        if (instanceLocation == null || instanceLocation.equals(this.appPaths.workingDir())) {
            LOGGER.warn("Instance location is '{}', resetting it to the default.", instanceLocation);
            get().setInstanceLocation(this.appPaths.instancesDir());
        }

        if (moveProxyPasswordToVault(get())) {
            try {
                this.store.save();
                this.store.save(); // The first save backs up the old file, password included. The second replaces that backup.
            } catch (IOException e) {
                LOGGER.error("Failed to save settings after moving the proxy password to the vault.", e);
            }
        }
    }

    private void attemptMigration() {
        // Don't migrate if the new settings file exists or the old one doesn't
        Path legacySettingsFile = this.appPaths.legacySettingsFile();
        if (Files.notExists(legacySettingsFile) || Files.exists(this.appPaths.settingsFile())) {
            return;
        }

        LOGGER.info("Migrating old settings to new format.");
        try {
            var originalSettingsRaw = Files.readString(legacySettingsFile);
            var originalSettings = new Gson().<HashMap<String, String>>fromJson(originalSettingsRaw, LEGACY_SETTINGS_TOKEN);
            var migrator = new SettingsMigrator(this.appPaths, originalSettings);
            var newSettings = migrator.migrate();

            // The legacy file kept the proxy password in plain text, keep it out of the new one
            moveProxyPasswordToVault(newSettings);

            // Save the new settings
            this.store.set(newSettings);

            // Delete the old settings file
            try {
                Files.deleteIfExists(legacySettingsFile);
            } catch (IOException e) {
                LOGGER.error("Failed to delete old settings file.", e);
            }
        } catch (Throwable e) {
            LOGGER.error("Failed to migrate settings file. Aborting migration process.", e);
            LOGGER.info("Deleting old settings file. If possible");
            try {
                Files.deleteIfExists(legacySettingsFile);
            } catch (IOException ex) {
                LOGGER.error("Failed to delete old settings file.", ex);
            }
        }
    }

    /// Older installs kept the proxy password in the settings file. If the vault fails it's left in place, so this
    /// retries on the next launch.
    ///
    /// @return whether a password was moved
    private boolean moveProxyPasswordToVault(SettingsData data) {
        var proxy = data.proxy();
        if (proxy == null || StringUtils.isEmpty(proxy.password())) {
            return false;
        }

        try {
            this.storage.vault().put(PROXY_PASSWORD_KEY, proxy.password());
            proxy.setPassword("");
            return true;
        } catch (VaultException e) {
            LOGGER.error("Failed to move the proxy password to the vault.", e);
            return false;
        }
    }

    private static boolean usesProxyAuth(SettingsData data) {
        var proxy = data.proxy();
        return proxy != null && !"none".equalsIgnoreCase(proxy.type()) && StringUtils.isNotEmpty(proxy.username());
    }

    public Path getInstancesDir() {
        return get().instanceLocation();
    }

    public static int getDefaultThreadLimit() {
        int defaultThreads = (Runtime.getRuntime().availableProcessors() / 2) - 1;
        if(defaultThreads < 2) defaultThreads = 2;
        return defaultThreads;
    }

    /// Read on every http request, so a settings file without a download section must not throw.
    public long getSpeedLimit() {
        var download = get().download();
        return download != null ? download.speedLimit() : 0;
    }

    public int getThreadLimit() {
        var download = get().download();
        return download != null ? download.threadLimit() : getDefaultThreadLimit();
    }

    public boolean ignoreForgeProcessorOutputHashes() {
        return get().workaround().ignoreForgeProcessorOutputHashes();
    }
}
