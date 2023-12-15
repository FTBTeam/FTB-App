package net.creeperhost.creeperlauncher.instance.importer;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.creeperhost.creeperlauncher.instance.importer.meta.SimpleInstanceInfo;
import net.creeperhost.creeperlauncher.instance.importer.providers.*;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class Importer {
    public static InstanceProvider factory(Providers provider) {
        return switch (provider) {
            case PRISM -> new PrismProvider();
            case MULTIMC -> new MultiMcProvider();
            case ATLAUNCHER -> new AtLauncherProvider();
            case GDLAUNCHER -> new GdLauncherProvider();
            case CURSEFORGE -> new CurseForgeProvider();
            case MODRINTH -> new ModrinthProvider();
        };
    }
    
    public static List<SimpleInstanceInfo> allInstances(Providers providerType, @Nullable Path providedPath) throws IOException {
        var provider = factory(providerType);
        
        var instancesPath = providedPath;
        if (instancesPath != null) {
            // Extended can be null annoyingly
            instancesPath = provider.extendedSourceLocation() != null ? provider.extendedSourceLocation() : provider.sourceLocation();
        }

        if (instancesPath == null || Files.notExists(instancesPath)) {
            // TODO: Real error
            return List.of();
        }
        
        return Files.list(instancesPath)
            .filter(Files::isDirectory)
            .map(provider::instance)
            .filter(Objects::nonNull)
            .toList();
    }
    
    public enum Providers {
        PRISM,
        MULTIMC,
        ATLAUNCHER,
        GDLAUNCHER,
        CURSEFORGE,
        MODRINTH;
        
        public static final List<Providers> VALUES = List.of(values());
    }
    
    public static class ProviderAdapter extends TypeAdapter<Providers> {
        @Override
        public void write(JsonWriter out, Providers value) throws IOException {
            out.value(value.name().toLowerCase());
        }
        
        @Override
        public Providers read(JsonReader in) throws IOException {
            return Providers.valueOf(in.nextString().toUpperCase());
        }
    }
}