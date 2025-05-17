package dev.ftb.app.util;

import com.google.gson.Gson;
import dev.ftb.app.api.handlers.profiles.StoreFtbAccountHandler;
import dev.ftb.app.storage.CredentialStorage;
import net.covers1624.quack.net.DownloadAction;
import dev.ftb.app.Constants;
import okhttp3.Request;

public class ModpackApiUtils {
    public static String API_TOKEN;
    
    public static PackType byteToPackType(byte packType) {
        return switch (packType) {
            case 0 -> PackType.MODPACK;
            case 1 -> PackType.CURSEFORGE;
            default -> PackType.UNKNOWN;
        };
    }

    public static String getModpacksApi() {
        return Constants.FTB_MODPACKS_API;
    }
    
    public static String getModpacksEndpoint(boolean isPrivate, byte packType) {
        PackType typeSlug = byteToPackType(packType);
        
        return getModpacksApi() + "/" + typeSlug.getSlug() + "/";
    }
    
    public static void injectBearerHeader(Request.Builder builder) {
        if (API_TOKEN != null) {
            builder.addHeader("Authorization", "Bearer " + API_TOKEN);
        }
    }
    
    public static void injectBearerHeader(DownloadAction action) {
        if (API_TOKEN != null) {
            action.addRequestHeader("Authorization", "Bearer " + API_TOKEN);
        }
    }
    
    public static void loadPrivateToken() {
        // Set the API key if we have it
        String accountData = CredentialStorage.getInstance().get("ftbAccount");
        if (accountData != null) {
            // Parse it
            try {
                StoreFtbAccountHandler.CompleteTokenData authData = new Gson().fromJson(accountData, StoreFtbAccountHandler.CompleteTokenData.class);
                if (authData != null && authData.idToken != null) {
                    API_TOKEN = authData.idToken;
                }
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    public enum PackType {
        MODPACK("modpack"),
        CURSEFORGE("curseforge"),
        UNKNOWN("unknown");
        
        private final String slug;
        
        PackType(String slug) {
            this.slug = slug;
        }
        
        public String getSlug() {
            return slug;
        }
    }
}
