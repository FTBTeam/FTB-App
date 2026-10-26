package dev.ftb.app.migration;

import dev.ftb.app.services.Services;

public interface Migration {
    String id();
    
    boolean migrate(Services services);
    
    default void cleanup(Services services) {
        // No cleanup required by default
    }
}
