package net.creeperhost.creeperlauncher.storage;

import com.google.gson.Gson;
import net.creeperhost.creeperlauncher.Constants;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Enumeration;
import java.util.HashMap;

/**
 * A non-ideal credential storage system backed by the users mac address.
 */
public class CredentialStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialStorage.class);
    private static CredentialStorage INSTANCE;
    
    private HashMap<String, String> credentials = new HashMap<>();
    private final byte[] macAddress;
    
    private CredentialStorage() {
        macAddress = getMacAddress();
        LOGGER.info("Loading credentials for user");
        try {
            // If it fails, it shouldn't be fatal. It will be a bit annoying for the user
            // but it's not the end of the world. 
            load();
        } catch (Exception e) {
            LOGGER.error("Failed to load credentials", e);
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
    private boolean save() {
        if (macAddress == null) {
            LOGGER.error("Failed to save credentials, mac address is null");
            return false;
        }
        
        // Convert the credentials to a json string
        String credentialsJson = "";
        try {
            credentialsJson = new Gson().toJson(credentials);
        } catch (Exception e) {
            LOGGER.error("Failed to convert credentials to json", e);
            return false;
        }
        
        // Encrypt the credentials
        try {
            // Create a SecretKey
            var key = generateKey();
            
            // Use the mac address as the key to encrypt the credentials
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            
            // Convert the encrypted credentials to a string
            byte[] encryptedBytes = cipher.doFinal(credentialsJson.getBytes());
            String encryptedCredentials = Base64.getEncoder().encodeToString(encryptedBytes);
            
            // Save the data to the file system
            Files.writeString(Constants.CREDENTIALS_FILE, encryptedCredentials);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to encrypt credentials", e);
            return false;
        }
    }
    
    private boolean load() {
        if (macAddress == null) {
            LOGGER.error("Failed to load credentials, mac address is null");
            return false;
        }
        
        // Load the encrypted credentials from the file system
        Path credentials = Constants.CREDENTIALS_FILE;
        if (!Files.exists(credentials)) {
            LOGGER.warn("Failed to load credentials, credentials file does not exist");
            return false;
        }
        
        // Decrypt the credentials
        try {
            String encryptedCredentials = Files.readString(credentials);
            var key = generateKey();
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedCredentials));
            String decryptedCredentials = new String(decryptedBytes);
            
            // Convert the decrypted credentials to a HashMap
            this.credentials = new Gson().fromJson(decryptedCredentials, HashMap.class);
        } catch (IOException | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeySpecException |
                 InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
        
        return true;
    }
    
    private SecretKeySpec generateKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(new String(macAddress).toCharArray(), "FTBAPP".getBytes(), 65536, 256);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    public static byte[] getMacAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface network = networkInterfaces.nextElement();
                byte[] mac = network.getHardwareAddress();
                if (mac != null && mac.length > 0 && !network.isLoopback() && !network.isVirtual() && !network.isPointToPoint() && !network.getName().substring(0,3).equals("ham") && !network.getName().substring(0, 3).equals("vir") && !network.getName().startsWith("docker") && !network.getName().startsWith("br-")) {
                    LOGGER.debug("Interface: " + network.getDisplayName() + " : " + network.getName());
                    var address = new byte[mac.length * 10];
                    for (int i = 0; i < address.length; i++) {
                        address[i] = mac[i - (Math.round(i / mac.length) * mac.length)];
                    }
                    return address;
                }
            }
        } catch (SocketException e) {
            LOGGER.warn("Exception getting MAC address", e);
        }

        LOGGER.warn("Failed to get MAC address, using default logindata key");
        return new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };
    }
}
