package dev.ftb.app.services.storage;

import org.jspecify.annotations.Nullable;

/// A [MasterKeyStore] that keeps the key in memory, for testing [EncryptedFileVault] without touching the OS.
public final class InMemoryKeyStore implements MasterKeyStore {
    private final String id;
    private final boolean secure;

    byte @Nullable [] key;
    boolean available = true;
    boolean failing;
    int saves;

    public InMemoryKeyStore(String id, boolean secure) {
        this.id = id;
        this.secure = secure;
    }

    @Override
    public byte @Nullable [] load() throws VaultException {
        if (failing) throw new VaultException("Simulated failure.");
        return key != null ? key.clone() : null;
    }

    @Override
    public void save(byte[] key) throws VaultException {
        if (failing) throw new VaultException("Simulated failure.");
        this.key = key.clone();
        saves++;
    }

    @Override
    public void delete() throws VaultException {
        if (failing) throw new VaultException("Simulated failure.");
        key = null;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    @Override
    public String id() {
        return id;
    }
}
