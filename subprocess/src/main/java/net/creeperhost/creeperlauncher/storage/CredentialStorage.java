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
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;

/**
 * TODO: When available, use the systems credential storage to store the users credentials.
 */
public class CredentialStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialStorage.class);
    
    private HashMap<String, String> credentials = new HashMap<>();
    private @Nullable String macAddress;
    
    public CredentialStorage() {
        try {
            macAddress = getMacAddress();
        } catch (SocketException | UnknownHostException e) {
            LOGGER.error("Failed to get mac address", e);
            macAddress = null;
        }
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
            Files.writeString(Constants.getDataDir().resolve("credentials.bat"), encryptedCredentials);
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
        Path credentials = Constants.getDataDir().resolve("credentials.bat");
        if (!Files.exists(credentials)) {
            LOGGER.error("Failed to load credentials, credentials file does not exist");
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
        KeySpec spec = new PBEKeySpec(macAddress.toCharArray(), "FTBAPP".getBytes(), 65536, 256);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }
    
    private static String getMacAddress() throws SocketException, UnknownHostException {
        NetworkInterface networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
        byte[] macAddressBytes = networkInterface.getHardwareAddress();
        return Arrays.toString(macAddressBytes);
    }
}
