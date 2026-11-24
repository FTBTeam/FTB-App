package dev.ftb.app;

public record AppFlags(
    boolean isDev,
    FrontendPlatform platform
) {
    public boolean isElectron() {
        return platform == FrontendPlatform.ELECTRON;
    }
    
    public boolean isOverwolf() {
        return platform == FrontendPlatform.OVERWOLF;
    }
    
    public enum FrontendPlatform {
        ELECTRON,
        OVERWOLF
    }
}
