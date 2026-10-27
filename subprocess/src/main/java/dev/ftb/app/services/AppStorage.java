package dev.ftb.app.services;

import dev.ftb.app.AppPaths;
import dev.ftb.app.services.storage.KVStorage;
import dev.ftb.app.services.storage.Vault;
import dev.ftb.app.storage.LegacyCredentialStorage;

public class AppStorage {
    private final Vault vault;
    private final KVStorage keyValue;
    
    @Deprecated
    private final LegacyCredentialStorage legacyCredentialStorage;
    
    public AppStorage(AppPaths paths) {
        vault = Vault.factory(paths);
        keyValue = new KVStorage(paths);
        keyValue.init(); // TODO: Move to a "services init" method?
        
        legacyCredentialStorage = new LegacyCredentialStorage(paths);
    }
    
    public Vault vault() {
        return vault;
    }
    
    public KVStorage keyValue() {
        return keyValue;
    }
    
    @Deprecated
    public LegacyCredentialStorage legacyCredentialStorage() {
        return legacyCredentialStorage;
    }
}
