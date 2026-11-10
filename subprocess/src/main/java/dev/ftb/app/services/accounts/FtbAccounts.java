package dev.ftb.app.services.accounts;

import dev.ftb.app.AppPaths;
import dev.ftb.app.services.AppStorage;
import dev.ftb.app.util.JsonStore;

public class FtbAccounts {
    private final JsonStore<FtbAccount> store;

    private final AppStorage appStorage;
    private final AppPaths appPaths;
    
    public FtbAccounts(AppPaths paths, AppStorage storage) {
        this.appPaths = paths;
        this.appStorage = storage;
        
        this.store = new JsonStore<>(
            paths.storageDir().resolve("ftb-accounts.json"),
            FtbAccount.class,
            FtbAccount::new,
            () -> new FtbAccount[0]
        );
    }
    
    
}
