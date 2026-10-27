package dev.ftb.app.services;

import dev.ftb.app.AppPaths;
import dev.ftb.app.services.storage.KVStorage;
import dev.ftb.app.services.storage.Vault;

public class AppStorage {
    private final Vault vault;
    private final KVStorage keyValue;
    
    public AppStorage(AppPaths paths) {
        vault = Vault.factory(paths);
        keyValue = new KVStorage(paths);
        keyValue.init(); // TODO: Move to a "services init" method?
    }
    
    public Vault vault() {
        return vault;
    }
    
    public KVStorage keyValue() {
        return keyValue;
    }
}
