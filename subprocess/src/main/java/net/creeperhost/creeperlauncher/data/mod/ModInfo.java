package net.creeperhost.creeperlauncher.data.mod;

import org.jetbrains.annotations.Nullable;

/**
 * Created by covers1624 on 5/9/23.
 */
public record ModInfo(
        long fileId,
        String fileName,
        @Nullable String version,
        boolean enabled,
        long size,
        @Nullable String sha1,
        @Nullable String murmurHash,
        @Nullable CurseMetadata curse
) {

}
