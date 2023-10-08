package net.creeperhost.creeperlauncher.instance.importer.meta;

import net.creeperhost.creeperlauncher.data.InstanceJson;
import org.jetbrains.annotations.Nullable;

public record MetaFinal(
   InstanceJson instanceJson,
   @Nullable CurseProjectMeta curseProject
) {}
