package net.creeperhost.creeperlauncher.instance.importer.meta;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.JsonAdapter;
import net.covers1624.quack.gson.PathTypeAdapter;
import net.creeperhost.creeperlauncher.minecraft.modloader.ModLoader;
import org.apache.tools.ant.taskdefs.Java;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

public record CurseForgeSettings(
    @Nullable Minecraft minecraft
) {
    public record Minecraft(
        Java java,
        @JsonAdapter(PathTypeAdapter.class) Path moddingFolder,
        String preferredRelease
    ) {}
    
    public record Java(
        int memory,
        String version
    ) {}
}
