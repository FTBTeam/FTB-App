package dev.ftb.app.services.storage;

import dev.ftb.app.AppPaths;
import net.covers1624.quack.platform.OperatingSystem;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/// Essentially a key-value store for secrets, protected by system-level storage.
///
/// Calls may block on first use, as the backing [MasterKeyStore] can show an OS prompt (unlocking a keyring, allowing
/// keychain access). Avoid calling it from anything latency sensitive.
public interface Vault {
    @Nullable String get(String key) throws VaultException;

    void put(String key, String value) throws VaultException;

    /// Stores all the entries in a single write, so either all of them are saved or none are.
    void putAll(Map<String, String> entries) throws VaultException;

    void remove(String key) throws VaultException;

    /// Wipes all stored secrets and the master key. Used to recover when the vault can no longer be decrypted, for
    /// example after the system keyring was reset.
    void reset() throws VaultException;

    /// Whether the secrets are protected by the OS. False when we had to use the [FallbackKeyStore].
    boolean isSecure() throws VaultException;

    /// The id of the [MasterKeyStore] protecting this vault, for logs and support.
    String backendName() throws VaultException;

    static Vault factory(AppPaths paths) {
        OperatingSystem os = OperatingSystem.current();
        MasterKeyStore primary;
        if (os.isWindows()) {
            primary = new WindowsDpapiKeyStore(paths.vaultKeyFile());
        } else if (os.isMacos()) {
            primary = new MacKeychainKeyStore();
        } else {
            primary = new SecretServiceKeyStore();
        }

        return new EncryptedFileVault(paths.vaultFile(), primary, new FallbackKeyStore(paths.vaultFallbackKeyFile()));
    }
}
