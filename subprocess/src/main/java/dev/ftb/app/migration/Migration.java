package dev.ftb.app.migration;

public interface Migration {
    String id();
    
    boolean migrate();
    
    default void cleanup() {
        // No cleanup required by default
    }
}
