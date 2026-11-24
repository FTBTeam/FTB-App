package dev.ftb.app.services.storage;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

/// AES-256-GCM helpers shared by the vault and the key stores that wrap keys themselves.
final class VaultCrypto {
    static final int KEY_LENGTH = 32;
    static final int NONCE_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private static final SecureRandom RANDOM = new SecureRandom();

    private VaultCrypto() {
    }

    record Sealed(byte[] nonce, byte[] data) {
    }

    static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    /// Encrypts with a fresh random nonce. A nonce must never be reused with the same key, so never pass one in.
    static Sealed seal(byte[] key, byte[] plaintext, byte[] aad) throws GeneralSecurityException {
        byte[] nonce = randomBytes(NONCE_LENGTH);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, nonce));
        cipher.updateAAD(aad);
        return new Sealed(nonce, cipher.doFinal(plaintext));
    }

    /// Decrypts and verifies. Throws if the key is wrong or the data or aad were tampered with.
    static byte[] open(byte[] key, byte[] nonce, byte[] data, byte[] aad) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, nonce));
        cipher.updateAAD(aad);
        return cipher.doFinal(data);
    }
}
