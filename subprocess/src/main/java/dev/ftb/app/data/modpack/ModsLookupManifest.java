package dev.ftb.app.data.modpack;

import java.util.List;

public record ModsLookupManifest(
    String notice,
    String status,
    List<Datum> data
) {
    public record Datum(
        long fileID,
        String name,
        String synopsis,
        String icon,
        String curseSlug,
        long curseProject,
        long curseFile,
        long stored,
        String filename,
        long murmurHash
    ) {}
}
