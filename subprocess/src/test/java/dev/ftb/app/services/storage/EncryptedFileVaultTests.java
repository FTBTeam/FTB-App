package dev.ftb.app.services.storage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class EncryptedFileVaultTests {
    private static final Gson GSON = new Gson();

    @TempDir
    Path dir;

    Path file;
    InMemoryKeyStore primary;
    InMemoryKeyStore fallback;

    @BeforeEach
    public void setup() {
        file = dir.resolve("vault.json");
        primary = new InMemoryKeyStore("primary", true);
        fallback = new InMemoryKeyStore("fallback", false);
    }

    private EncryptedFileVault vault() {
        return new EncryptedFileVault(file, primary, fallback);
    }

    private JsonObject readFile() throws IOException {
        return GSON.fromJson(Files.readString(file), JsonObject.class);
    }

    @Test
    public void testRoundTripAcrossInstances() throws Exception {
        vault().put("token", "secret-value");

        EncryptedFileVault reopened = vault();
        assertEquals("secret-value", reopened.get("token"));
        assertNull(reopened.get("missing"));
    }

    @Test
    public void testFileDoesNotContainPlaintext() throws Exception {
        vault().put("token", "secret-value");

        String contents = Files.readString(file);
        assertFalse(contents.contains("secret-value"));
        assertFalse(contents.contains("token"));
    }

    @Test
    public void testUsesPrimaryWhenAvailable() throws Exception {
        EncryptedFileVault vault = vault();

        assertEquals("primary", vault.backendName());
        assertTrue(vault.isSecure());
        assertNotNull(primary.key);
        assertNull(fallback.key);
        assertEquals("primary", readFile().get("keyStore").getAsString());
    }

    @Test
    public void testUsesFallbackWhenPrimaryUnavailable() throws Exception {
        primary.available = false;
        EncryptedFileVault vault = vault();
        vault.put("token", "value");

        assertEquals("fallback", vault.backendName());
        assertFalse(vault.isSecure());
        assertNull(primary.key);
        assertEquals("value", vault().get("token"));
    }

    @Test
    public void testFallsBackWhenPrimaryFailsOnCreate() throws Exception {
        primary.failing = true;

        assertEquals("fallback", vault().backendName());
    }

    @Test
    public void testDoesNotDowngradeExistingVault() throws Exception {
        vault().put("token", "value");
        String before = Files.readString(file);

        primary.failing = true;
        EncryptedFileVault vault = vault();
        assertThrows(VaultException.class, () -> vault.get("token"));
        assertThrows(VaultException.class, () -> vault.put("other", "value"));

        assertEquals(before, Files.readString(file));
        assertNull(fallback.key);
    }

    @Test
    public void testMissingKeyFailsInsteadOfRecreating() throws Exception {
        vault().put("token", "value");
        primary.key = null;

        assertThrows(VaultException.class, () -> vault().get("token"));
        assertEquals(1, primary.saves);
    }

    @Test
    public void testWrongKeyFails() throws Exception {
        vault().put("token", "value");
        primary.key = VaultCrypto.randomBytes(VaultCrypto.KEY_LENGTH);

        assertThrows(VaultException.class, () -> vault().get("token"));
    }

    @Test
    public void testTamperedKeyStoreHeaderFails() throws Exception {
        vault().put("token", "value");
        // Give the fallback the same key, so only the header binding can catch the swap
        fallback.key = primary.key.clone();

        JsonObject json = readFile();
        json.addProperty("keyStore", "fallback");
        Files.writeString(file, GSON.toJson(json));

        assertThrows(VaultException.class, () -> vault().get("token"));
    }

    @Test
    public void testUnknownKeyStoreFails() throws Exception {
        vault().put("token", "value");

        JsonObject json = readFile();
        json.addProperty("keyStore", "something-else");
        Files.writeString(file, GSON.toJson(json));

        assertThrows(VaultException.class, () -> vault().get("token"));
    }

    @Test
    public void testCorruptFileFails() throws Exception {
        Files.writeString(file, "{ not json");

        assertThrows(VaultException.class, () -> vault().get("token"));
    }

    @Test
    public void testNonceChangesOnEveryWrite() throws Exception {
        EncryptedFileVault vault = vault();
        vault.put("token", "value");
        String first = readFile().get("nonce").getAsString();
        vault.put("token", "value");
        String second = readFile().get("nonce").getAsString();

        assertNotEquals(first, second);
    }

    @Test
    public void testRemove() throws Exception {
        EncryptedFileVault vault = vault();
        vault.put("a", "1");
        vault.put("b", "2");
        vault.remove("a");
        vault.remove("never-existed");

        EncryptedFileVault reopened = vault();
        assertNull(reopened.get("a"));
        assertEquals("2", reopened.get("b"));
    }

    @Test
    public void testReusesLeftoverKey() throws Exception {
        byte[] leftover = VaultCrypto.randomBytes(VaultCrypto.KEY_LENGTH);
        primary.key = leftover.clone();

        vault().put("token", "value");

        assertArrayEquals(leftover, primary.key);
        assertEquals(0, primary.saves);
    }

    @Test
    public void testResetRecoversFromLostKey() throws Exception {
        vault().put("token", "value");
        primary.key = null;
        EncryptedFileVault vault = vault();
        assertThrows(VaultException.class, () -> vault.get("token"));

        vault.reset();

        assertFalse(Files.exists(file));
        assertNull(vault.get("token"));
        vault.put("token", "new-value");
        assertEquals("new-value", vault().get("token"));
    }

    @Test
    public void testResetLetsPrimaryBeChosenAgain() throws Exception {
        primary.available = false;
        EncryptedFileVault vault = vault();
        assertEquals("fallback", vault.backendName());

        vault.reset();
        primary.available = true;

        assertEquals("primary", vault.backendName());
        assertNull(fallback.key);
    }

    @Test
    public void testFallbackKeyStoreRoundTrip() throws Exception {
        FallbackKeyStore store = new FallbackKeyStore(dir.resolve("vault.fallback.key"));
        assertNull(store.load());

        byte[] key = VaultCrypto.randomBytes(VaultCrypto.KEY_LENGTH);
        store.save(key);

        assertArrayEquals(key, new FallbackKeyStore(dir.resolve("vault.fallback.key")).load());
        assertFalse(Files.readString(dir.resolve("vault.fallback.key")).contains(java.util.Base64.getEncoder().encodeToString(key)));

        store.delete();
        assertNull(store.load());
    }

    @Test
    public void testVaultWithRealFallbackKeyStore() throws Exception {
        primary.available = false;
        Path keyFile = dir.resolve("vault.fallback.key");
        new EncryptedFileVault(file, primary, new FallbackKeyStore(keyFile)).put("token", "value");

        assertEquals("value", new EncryptedFileVault(file, primary, new FallbackKeyStore(keyFile)).get("token"));
    }
}
