package dev.ftb.app.data.mod;

import org.jetbrains.annotations.Nullable;

public record ModInfo(
        long fileId,
        String fileName,
        @Nullable String version,
        boolean enabled,
        long size,
        @Nullable String sha1,
        long murmurHash,
        @Nullable CurseMetadata curse
) {

}
