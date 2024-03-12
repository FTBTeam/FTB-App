package net.creeperhost.creeperlauncher.util;

import net.covers1624.quack.net.DownloadAction;
import net.creeperhost.creeperlauncher.Constants;
import net.creeperhost.creeperlauncher.storage.UserApiCredentials;
import okhttp3.Request;

public class ModpacksChUtils {
    public static String API_TOKEN = "";
    
    public static UserApiCredentials USER_PROVIDED_CREDENTIALS = null;

    public static PackType byteToPackType(byte packType) {
        return switch (packType) {
            case 0 -> PackType.MODPACK;
            case 1 -> PackType.CURSEFORGE;
            default -> PackType.UNKNOWN;
        };
    }

    public static String getModpacksApi() {
        if (USER_PROVIDED_CREDENTIALS != null) {
            return USER_PROVIDED_CREDENTIALS.apiUrl();
        }
        
        return Constants.IS_DEV_MODE ? Constants.CREEPERHOST_MODPACK_STAGING : Constants.CREEPERHOST_MODPACK;
    }

    public static String getModEndpoint() {
        boolean usePublic = USER_PROVIDED_CREDENTIALS == null || USER_PROVIDED_CREDENTIALS.supportsPublicPrefix();
        return getModpacksApi() + (usePublic ? "/public" : "") + "/mod/";
    }
    
    public static String getPublicApi() {
        if (USER_PROVIDED_CREDENTIALS != null && !USER_PROVIDED_CREDENTIALS.supportsPublicPrefix()) {
            return getModpacksApi() + "/";
        }
        
        return getModpacksApi() + "/public/";
    }

    public static String getModpacksEndpoint(boolean isPrivate, byte packType) {
        String key = "public";
        PackType typeSlug = byteToPackType(packType);

        if (typeSlug == PackType.CURSEFORGE) {
            // CurseForge has no private packs.
            isPrivate = false;
        }

        if (isPrivate) {
            if (USER_PROVIDED_CREDENTIALS == null && !API_TOKEN.isEmpty()) {
                key = API_TOKEN;
            } else if (USER_PROVIDED_CREDENTIALS != null) {
                if (USER_PROVIDED_CREDENTIALS.usesBearerAuth()) {
                    // No public or token needed for bearer auth.
                    return getModpacksApi() + "/" + typeSlug.getSlug() + "/";
                }
                
                key = USER_PROVIDED_CREDENTIALS.apiSecret();
            }
        }
        
        return getModpacksApi() + "/" + key + "/" + typeSlug.getSlug() + "/";
    }
    
    public static void injectBearerHeader(Request.Builder builder) {
        if (USER_PROVIDED_CREDENTIALS != null && USER_PROVIDED_CREDENTIALS.usesBearerAuth()) {
            builder.addHeader("Authorization", "Bearer " + USER_PROVIDED_CREDENTIALS.apiSecret());
        }
    }
    
    public static void injectBearerHeader(DownloadAction action) {
        if (USER_PROVIDED_CREDENTIALS != null && USER_PROVIDED_CREDENTIALS.usesBearerAuth()) {
            action.addRequestHeader("Authorization", "Bearer " + USER_PROVIDED_CREDENTIALS.apiSecret());
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
