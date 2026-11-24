package dev.ftb.app.services.storage;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import dev.ftb.app.util.SafeFiles;
import org.jspecify.annotations.Nullable;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Base64;

/// When all other options fail, we fallback to a slightly improved version of the legacy credential store. This should 
/// only happen on Linux where the OS does not implement a standard credential store.
///
/// We derive the master key from a machine fingerprint and store it in a file in the user's home directory. This is not
/// a secure option but it's better than nothing.
public final class FallbackKeyStore implements MasterKeyStore {
    private static final Gson GSON = new Gson();
    private static final byte[] AAD = "ftb-vault-fallback:1".getBytes(StandardCharsets.UTF_8);
    private static final int SALT_LENGTH = 32;
    private static final int ITERATIONS = 210_000;

    private final Path file;

    public FallbackKeyStore(Path file) {
        this.file = file;
    }

    @Override
    public byte @Nullable [] load() throws VaultException {
        if (!Files.exists(file)) return null;

        try {
            KeyFile keyFile = GSON.fromJson(Files.readString(file), KeyFile.class);
            if (keyFile == null || keyFile.salt() == null || keyFile.nonce() == null || keyFile.key() == null) {
                throw new VaultException("Fallback key file is malformed.");
            }

            Base64.Decoder decoder = Base64.getDecoder();
            byte[] wrappingKey = deriveWrappingKey(decoder.decode(keyFile.salt()));
            return VaultCrypto.open(wrappingKey, decoder.decode(keyFile.nonce()), decoder.decode(keyFile.key()), AAD);
        } catch (IOException | JsonParseException | IllegalArgumentException | GeneralSecurityException ex) {
            throw new VaultException("Failed to load fallback key.", ex);
        }
    }

    @Override
    public void save(byte[] key) throws VaultException {
        try {
            byte[] salt = VaultCrypto.randomBytes(SALT_LENGTH);
            VaultCrypto.Sealed sealed = VaultCrypto.seal(deriveWrappingKey(salt), key, AAD);
            Base64.Encoder encoder = Base64.getEncoder();
            KeyFile keyFile = new KeyFile(encoder.encodeToString(salt), encoder.encodeToString(sealed.nonce()), encoder.encodeToString(sealed.data()));
            SafeFiles.writeAtomically(file, GSON.toJson(keyFile));
        } catch (IOException | GeneralSecurityException ex) {
            throw new VaultException("Failed to save fallback key.", ex);
        }
    }

    @Override
    public void delete() throws VaultException {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ex) {
            throw new VaultException("Failed to delete fallback key.", ex);
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public String id() {
        return "fallback";
    }

    private static byte[] deriveWrappingKey(byte[] salt) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(machineFingerprint().toCharArray(), salt, ITERATIONS, VaultCrypto.KEY_LENGTH * 8);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    /// Only values that are stable for the life of an install.
    private static String machineFingerprint() {
        return String.join("|",
                "ftb-app",
                System.getProperty("user.name", ""),
                System.getProperty("user.home", ""),
                System.getProperty("os.name", ""),
                System.getProperty("os.arch", ""),
                linuxMachineId()
        );
    }

    /// Seeing as this is going to typically be used on linux, we might as well pull in the machine-id if it exists. 
    /// This is a stable value that is unique to the machine.
    private static String linuxMachineId() {
        try {
            Path machineId = Path.of("/etc/machine-id");
            return Files.isReadable(machineId) ? Files.readString(machineId).trim() : "";
        } catch (IOException ex) {
            return "";
        }
    }

    private record KeyFile(String salt, String nonce, String key) {
    }
}
