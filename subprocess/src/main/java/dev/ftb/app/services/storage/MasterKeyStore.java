package dev.ftb.app.services.storage;

import org.jspecify.annotations.Nullable;

/// Holds the single master key that encrypts the [EncryptedFileVault]. Each implementation stores it somewhere
/// different, ideally somewhere the OS protects.
/// 
/// Implementation note: The KeyStores themselves were created by Claude Opus 5.5 and manually reviewed and testing.
/// These implementations are likely not perfect, but they are better than what I could find online via legacy and 
/// outdated libraries and from what I could have done myself having a limited understanding of the underlying C APIs.
public interface MasterKeyStore {
    /// @return the stored key, or `null` if no key has been stored yet
    byte @Nullable [] load() throws VaultException;

    /// Stores the key, replacing any existing one.
    void save(byte[] key) throws VaultException;

    /// Removes the stored key, if any.
    void delete() throws VaultException;

    /// A cheap probe for whether this store can work here, such as the native library loading. Must not prompt the
    /// user or block.
    boolean isAvailable();

    /// Whether the key is protected by the OS rather than just obfuscated.
    boolean isSecure();

    /// Stable id written into the vault file, so we can tell which store a vault belongs to. Never change these.
    String id();
}
