package net.creeperhost.creeperlauncher.instance.importer;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.creeperhost.creeperlauncher.instance.importer.providers.CurseForgeProvider;
import net.creeperhost.creeperlauncher.instance.importer.providers.InstanceProvider;

import java.io.IOException;
import java.util.List;

public class Importer {
    public static InstanceProvider factory(Providers provider) {
        return switch (provider) {
//            case PRISM -> new PrismProvider();
//            case MULTIMC -> new MultiMcProvider();
//            case ATLAUNCHER -> new AtLauncherProvider();
//            case GDLAUNCHER -> new GdLauncherProvider();
            case CURSEFORGE -> new CurseForgeProvider();
//            case MODRINTH -> new ModrinthProvider();
        };
    }
    
    public enum Providers {
//        PRISM,
//        MULTIMC,
//        ATLAUNCHER,
//        GDLAUNCHER,
        CURSEFORGE,
//        MODRINTH;
        ;
        
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