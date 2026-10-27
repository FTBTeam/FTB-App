package dev.ftb.app.services.storage;

import dev.ftb.app.AppPaths;

/// Essentially a key-value store backed by system-level storage.
public interface Vault {
    String get(String key);
    
    boolean put(String key, String value);
    
    static Vault factory(AppPaths paths) {
        // TODO: Implement a vault factory that switches the backing store depending on the OS.
//        throw new UnsupportedOperationException("Vault is not yet implemented.");
        return new Vault() {
            @Override
            public String get(String key) {
                return "";
            }

            @Override
            public boolean put(String key, String value) {
                return false;
            }
        };
    }
}
