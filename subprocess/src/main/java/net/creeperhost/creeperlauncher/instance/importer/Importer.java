package net.creeperhost.creeperlauncher.instance.importer;

import net.creeperhost.creeperlauncher.instance.importer.providers.*;

public class Importer {
    public static InstanceProvider factory(Providers provider) {
        return switch (provider) {
            case PRISM -> new PrismProvider();
            case MULTIMC -> new MultiMcProvider();
            case ATLAUNCHER -> new AtLauncherProvider();
            case GDLAUNCHER -> new GdLauncherProvider();
            case CURSEFORGE -> new CurseForgeProvider();
        };
    }
    
    public enum Providers {
        PRISM,
        MULTIMC,
        ATLAUNCHER,
        GDLAUNCHER,
        CURSEFORGE,
    }
}