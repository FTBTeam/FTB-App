package net.creeperhost.creeperlauncher.data.mod;

import org.jetbrains.annotations.Nullable;

/**
 * Created by covers1624 on 2/10/23.
 */
public record CurseMetadata(
        long curseProject,
        long curseFile,
        @Nullable String name,
        @Nullable String synopsis,
        @Nullable String icon
) { }
