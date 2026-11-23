package dev.ftb.app.util;

import dev.ftb.app.Constants;

@ReleaseBlocking(reason = "This whole class needs to move into a service.")
public class ModpackApiUtils {
    public static PackType byteToPackType(byte packType) {
        return switch (packType) {
            case 0 -> PackType.MODPACK;
            case 1 -> PackType.CURSEFORGE;
            default -> PackType.UNKNOWN;
        };
    }

    @Deprecated
    public static String getModpacksApi() {
        return Constants.FTB_MODPACKS_API;
    }
    
    public static String getModpacksEndpoint(boolean isPrivate, byte packType) {
        PackType typeSlug = byteToPackType(packType);
        
        return getModpacksApi() + "/" + typeSlug.getSlug() + "/";
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
