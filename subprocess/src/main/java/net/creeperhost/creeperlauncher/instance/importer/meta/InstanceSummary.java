package net.creeperhost.creeperlauncher.instance.importer.meta;

import com.google.gson.annotations.JsonAdapter;
import net.covers1624.quack.gson.PathTypeAdapter;

import java.nio.file.Path;

public record InstanceSummary(
        String name,
        @JsonAdapter (PathTypeAdapter.class) Path dataLocation,
        String minecraftVersion,
        String javaVersion
) { }
