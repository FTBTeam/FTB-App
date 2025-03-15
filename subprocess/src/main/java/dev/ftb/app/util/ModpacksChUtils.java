package dev.ftb.app.util;

import net.covers1624.quack.net.DownloadAction;
import dev.ftb.app.Constants;
import okhttp3.Request;

public class ModpacksChUtils {
    public static String API_TOKEN = "";
    
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
//        if (USER_PROVIDED_CREDENTIALS != null && USER_PROVIDED_CREDENTIALS.usesBearerAuth()) {
//            builder.addHeader("Authorization", "Bearer " + USER_PROVIDED_CREDENTIALS.apiSecret());
//        }
    }
    
    public static void injectBearerHeader(DownloadAction action) {
//        if (USER_PROVIDED_CREDENTIALS != null && USER_PROVIDED_CREDENTIALS.usesBearerAuth()) {
//            action.addRequestHeader("Authorization", "Bearer " + USER_PROVIDED_CREDENTIALS.apiSecret());
//        }
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
