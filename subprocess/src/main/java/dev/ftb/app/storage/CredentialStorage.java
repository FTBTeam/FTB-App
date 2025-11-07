package dev.ftb.app.storage;

import com.google.gson.Gson;
import dev.ftb.app.AppMain;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.*;

/**
 * A non-secure storage for user credentials. The key is generated from consistent system information
 * to effectively fingerprint the file to the user's machine. This is primarily to prevent relatively 
 * simple grab and dump style attacks. Ultimately, this information should be store on the users systems
 * keychain, windows credential manager, or equivalent but this is a decent stop gap.
 */
public class CredentialStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialStorage.class);
    private static CredentialStorage INSTANCE;
    
    private final String encryptionKey;
    
    private HashMap<String, String> credentials = new HashMap<>();
    
    private CredentialStorage() {
        encryptionKey = generateEncryptionKey();
        LOGGER.info("Loading credentials for user");
        try {
            // If it fails, it shouldn't be fatal. It will be a bit annoying for the user,
            // but it's not the end of the world. 
            load();
        } catch (NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException |
                 InvalidKeySpecException | BadPaddingException | InvalidAlgorithmParameterException |
                 InvalidKeyException | IOException e) {
            // In some cases, we want to just delete the file to reset the credentials.
            // In others, it's a legitimate error we can't recover from.
            if (e instanceof BadPaddingException) {
                LOGGER.warn("Failed to load credentials, deleting credentials file to reset", e);
                try {
                    Files.deleteIfExists(AppMain.paths().credentialsFiles());
                } catch (IOException ioException) {
                    // RIP.
                    LOGGER.error("Failed to delete credentials file", ioException);
                }
            } else {
                LOGGER.error("Failed to load credentials", e);
            }
        }
    }
    
    public static CredentialStorage getInstance() {
        if (CredentialStorage.INSTANCE == null) {
            CredentialStorage.INSTANCE = new CredentialStorage();
        }
        
        return CredentialStorage.INSTANCE;
    }
    
    public @Nullable String get(String key) {
        return credentials.get(key);
    }
    
    public void set(String key, String value) {
        credentials.put(key, value);
        save();
    }
    
    public void setMultiple(Map<String, String> items) {
        credentials.putAll(items);
        save();
    }
    
    public void remove(String key) {
        credentials.remove(key);
        save();
    }
    
    public HashMap<String, String> getCredentials() {
        return credentials;
    }

    /**
     * Use the systems mac address to encrypt the users credentials.
     */
    private void save() {        
        // Convert the credentials to a json string
        String credentialsJson = "";
        try {
            credentialsJson = new Gson().toJson(credentials);
        } catch (Exception e) {
            LOGGER.error("Failed to convert credentials to json", e);
            return;
        }
        
        // Encrypt the credentials
        try {
            // Create a SecretKey
            var key = generateKey();
            
            var userHome = System.getProperty("user.home");
            // Use the user home directory as the IV
            byte[] iv = userHome.getBytes();
            
            GCMParameterSpec spec = new GCMParameterSpec(128, iv);
            
            // Use the mac address as the key to encrypt the credentials
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            
            // Convert the encrypted credentials to a string
            byte[] encryptedBytes = cipher.doFinal(credentialsJson.getBytes());
            String encryptedCredentials = Base64.getEncoder().encodeToString(encryptedBytes);
            
            // Save the data to the file system
            Files.writeString(AppMain.paths().credentialsFiles(), encryptedCredentials);
        } catch (Exception e) {
            LOGGER.error("Failed to encrypt credentials", e);
        }
    }
    
    private void load() throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, IOException {        
        // Load the encrypted credentials from the file system
        Path credentials = AppMain.paths().credentialsFiles();
        if (!Files.exists(credentials)) {
            LOGGER.warn("Failed to load credentials, credentials file does not exist");
            return;
        }
        
        // Decrypt the credentials
        String encryptedCredentials = Files.readString(credentials);

        var userHome = System.getProperty("user.home");
        // Use the user home directory as the IV
        byte[] iv = userHome.getBytes();

        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        
        var key = generateKey();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedCredentials));
        String decryptedCredentials = new String(decryptedBytes);
        
        // Convert the decrypted credentials to a HashMap
        this.credentials = new Gson().fromJson(decryptedCredentials, HashMap.class);
    }
    
    private SecretKeySpec generateKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(encryptionKey.toCharArray(), "FTBAPP".getBytes(), 65536, 256);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    private String generateEncryptionKey() {
        String appName = "FTBApp";
        String storageName = "CredentialStorageV1";
        
        String userHome = System.getProperty("user.home");
        String osName = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");
        String timeZone = System.getProperty("user.timezone");
        
        List<String> components = shuffleBasedOnUserHome(userHome, appName, storageName, userHome, osName, osArch, timeZone);
        StringBuilder keyBuilder = new StringBuilder();
        for (String component : components) {
            keyBuilder.append(component).append("|");
        }
        
        return keyBuilder.toString();
    }

    /**
     * This should create a seed that produces a consistent shuffle based on the user's home directory.
     * This way the order of the components is unique to the user but consistent across runs.
     * 
     * @param userHome The user's home directory
     * @param components The components to shuffle
     * @return A shuffled list of components
     */
    private List<String> shuffleBasedOnUserHome(String userHome, String... components) {
        List<String> componentList = new ArrayList<>(Arrays.asList(components));
        long seed = 0;
        for (char c : userHome.toCharArray()) {
            seed += c;
        }
        Collections.shuffle(componentList, new Random(seed));
        return componentList;
    }
}
