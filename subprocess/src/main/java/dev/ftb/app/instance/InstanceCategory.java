package dev.ftb.app.instance;

import java.util.UUID;

public class InstanceCategory {
    public static final InstanceCategory DEFAULT = new InstanceCategory(
        UUID.fromString("02c2cc8e-0b8d-45d0-ac1d-7e32b1c21cb0"),
        "My Instances"
    );
    
    private final UUID uuid;
    private String name;

    public InstanceCategory(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }
    
    // New category constructor
    public InstanceCategory(String name) {
        this(UUID.randomUUID(), name);
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
