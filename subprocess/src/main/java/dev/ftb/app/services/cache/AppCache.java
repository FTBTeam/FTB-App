package dev.ftb.app.services.cache;

import dev.ftb.app.AppPaths;
import dev.ftb.app.services.AppHttp;
import dev.ftb.app.services.AppThreads;
import dev.ftb.app.storage.settings.Settings;

public class AppCache {    
    private final LocalCache local;
    private final CurseMetadataCache curseMetadata;
    private final ModVersionCache modVersionCache;
    
    public AppCache(AppPaths appPaths, Settings settings, AppThreads threads, AppHttp appHttp) {        
        this.local = new LocalCache(settings);
        this.modVersionCache = new ModVersionCache(appPaths, threads, appHttp);
        this.curseMetadata = new CurseMetadataCache(appPaths, appHttp, this.modVersionCache);
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
