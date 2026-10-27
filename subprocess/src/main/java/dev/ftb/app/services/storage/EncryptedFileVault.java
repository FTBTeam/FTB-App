package dev.ftb.app.services.storage;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import dev.ftb.app.util.SafeFiles;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/// A [Vault] stored as a single AES-GCM encrypted file, whose key is held by a [MasterKeyStore].
///
/// The vault is a simple Json file that contains encrypted key-value pairs.
/// The vault is backed by a [MasterKeyStore] that provides a key to encrypt and decrypt the vault file.
/// 
/// Note: This is very likely not thread-safe.
public final class EncryptedFileVault implements Vault {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() { }.getType();

    private static final int VERSION = 1;

    private final Path file;
    private final MasterKeyStore primary;
    private final MasterKeyStore fallback;

    private @Nullable Unlocked state;

    public EncryptedFileVault(Path file, MasterKeyStore primary, MasterKeyStore fallback) {
        this.file = file;
        this.primary = primary;
        this.fallback = fallback;
    }

    @Override
    public synchronized @Nullable String get(String key) throws VaultException {
        return unlock().data().get(key);
    }

    @Override
    public synchronized void put(String key, String value) throws VaultException {
        Unlocked unlocked = unlock();
        Map<String, String> data = new HashMap<>(unlocked.data());
        data.put(key, value);
        write(unlocked.store(), unlocked.key(), data);
        state = new Unlocked(unlocked.store(), unlocked.key(), data);
    }

    @Override
    public synchronized void remove(String key) throws VaultException {
        Unlocked unlocked = unlock();
        if (!unlocked.data().containsKey(key)) return;

        Map<String, String> data = new HashMap<>(unlocked.data());
        data.remove(key);
        write(unlocked.store(), unlocked.key(), data);
        state = new Unlocked(unlocked.store(), unlocked.key(), data);
    }

    @Override
    public synchronized void reset() throws VaultException {
        LOGGER.warn("Resetting vault, all stored secrets will be lost.");
        state = null;
        for (MasterKeyStore store : new MasterKeyStore[] { primary, fallback }) {
            try {
                store.delete();
            } catch (VaultException ex) {
                LOGGER.warn("Failed to delete master key from {}.", store.id(), ex);
            }
        }

        try {
            Files.deleteIfExists(file);
        } catch (IOException ex) {
            throw new VaultException("Failed to delete vault file.", ex);
        }
    }

    @Override
    public synchronized boolean isSecure() throws VaultException {
        return unlock().store().isSecure();
    }

    @Override
    public synchronized String backendName() throws VaultException {
        return unlock().store().id();
    }

    private Unlocked unlock() throws VaultException {
        if (state == null) {
            state = Files.exists(file) ? open() : create();
        }
        return state;
    }

    private Unlocked open() throws VaultException {
        VaultFile vaultFile;
        try {
            vaultFile = GSON.fromJson(Files.readString(file), VaultFile.class);
        } catch (IOException | JsonParseException ex) {
            throw new VaultException("Failed to read vault file.", ex);
        }
        if (vaultFile == null || vaultFile.keyStore() == null || vaultFile.nonce() == null || vaultFile.data() == null) {
            throw new VaultException("Vault file is malformed.");
        }
        if (vaultFile.version() != VERSION) {
            throw new VaultException("Unsupported vault version " + vaultFile.version() + ".");
        }

        MasterKeyStore store = storeFor(vaultFile.keyStore());
        if (store == null) {
            throw new VaultException("Vault was created with key store '" + vaultFile.keyStore() + "', which isn't supported here.");
        }

        byte[] key = store.load();
        if (key == null) {
            throw new VaultException("Master key is missing from '" + store.id() + "', the vault can't be decrypted.");
        }

        Map<String, String> data;
        try {
            Base64.Decoder decoder = Base64.getDecoder();
            byte[] plaintext = VaultCrypto.open(key, decoder.decode(vaultFile.nonce()), decoder.decode(vaultFile.data()), aad(store));
            data = GSON.fromJson(new String(plaintext, StandardCharsets.UTF_8), MAP_TYPE);
        } catch (GeneralSecurityException | IllegalArgumentException | JsonParseException ex) {
            throw new VaultException("Failed to decrypt vault using '" + store.id() + "'.", ex);
        }
        
        return new Unlocked(store, key, data != null ? data : Map.of());
    }

    private Unlocked create() throws VaultException {
        MasterKeyStore store = primary.isAvailable() ? primary : fallback;
        byte[] key;
        try {
            key = loadOrCreateKey(store);
        } catch (VaultException ex) {
            if (store == fallback) throw ex;

            // Nothing is stored yet, so falling back here can't lose any data
            LOGGER.warn("Key store '{}' failed, falling back to '{}'.", store.id(), fallback.id(), ex);
            store = fallback;
            key = loadOrCreateKey(store);
        }

        if (!store.isSecure()) {
            LOGGER.warn("No secure key store available, secrets will only be obfuscated.");
        }

        // Write straight away so the choice of key store sticks
        Map<String, String> data = Map.of();
        write(store, key, data);
        LOGGER.info("Created new vault using '{}'.", store.id());
        return new Unlocked(store, key, data);
    }

    private static byte[] loadOrCreateKey(MasterKeyStore store) throws VaultException {
        // A key can be left over from a vault file that was deleted, reuse it rather than orphaning it
        byte[] key = store.load();
        if (key != null && key.length == VaultCrypto.KEY_LENGTH) {
            return key;
        }

        key = VaultCrypto.randomBytes(VaultCrypto.KEY_LENGTH);
        store.save(key);
        return key;
    }

    private void write(MasterKeyStore store, byte[] key, Map<String, String> data) throws VaultException {
        try {
            VaultCrypto.Sealed sealed = VaultCrypto.seal(key, GSON.toJson(data, MAP_TYPE).getBytes(StandardCharsets.UTF_8), aad(store));
            Base64.Encoder encoder = Base64.getEncoder();
            VaultFile vaultFile = new VaultFile(VERSION, store.id(), encoder.encodeToString(sealed.nonce()), encoder.encodeToString(sealed.data()));
            SafeFiles.writeAtomically(file, GSON.toJson(vaultFile));
        } catch (GeneralSecurityException | IOException ex) {
            throw new VaultException("Failed to write vault.", ex);
        }
    }

    private @Nullable MasterKeyStore storeFor(String id) {
        if (primary.id().equals(id)) return primary;
        if (fallback.id().equals(id)) return fallback;
        return null;
    }

    /// Binds the header to the ciphertext, so changing the version or key store in the file fails decryption.
    private static byte[] aad(MasterKeyStore store) {
        return ("ftb-vault:" + VERSION + ":" + store.id()).getBytes(StandardCharsets.UTF_8);
    }

    private record Unlocked(MasterKeyStore store, byte[] key, Map<String, String> data) {
    }

    private record VaultFile(int version, String keyStore, String nonce, String data) {
    }
}
