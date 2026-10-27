package dev.ftb.app.services.cache;

import dev.ftb.app.AppPaths;
import dev.ftb.app.services.AppThreads;
import dev.ftb.app.storage.settings.Settings;

public class AppCache {
    private final Settings settings;
    private final AppPaths appPaths;
    
    private LocalCache local;
    private final CurseMetadataCache curseMetadata;
    private final ModVersionCache modVersionCache;
    
    public AppCache(AppPaths appPaths, Settings settings, AppThreads threads) {
        this.settings = settings;
        this.appPaths = appPaths;
        
        this.local = new LocalCache(this.settings);
        this.modVersionCache = new ModVersionCache(this.appPaths, threads);
        this.curseMetadata = new CurseMetadataCache(this.appPaths, this.modVersionCache);
    }

    public LocalCache local() {
        return local;
    }

    public CurseMetadataCache curseMetadata() {
        return curseMetadata;
    }
    
    public ModVersionCache modVersionCache() {
        return modVersionCache;
    }
}
