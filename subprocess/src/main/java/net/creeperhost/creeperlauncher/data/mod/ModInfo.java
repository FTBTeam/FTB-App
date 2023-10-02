package net.creeperhost.creeperlauncher.data.mod;

import org.jetbrains.annotations.Nullable;

/**
 * Created by covers1624 on 5/9/23.
 */
public record ModInfo(long fileId, String fileName, @Nullable String version, boolean enabled, long size, @Nullable String sha1, long curseProject, long curseFile, @Nullable String name, @Nullable String description) {

    public ModInfo(long fileId, String fileName, @Nullable String version, boolean enabled, long size, @Nullable String sha1, long curseProject, long curseFile) {
        this(fileId, fileName, version, enabled, size, sha1, curseProject, curseFile, null, null);
    }

    public ModInfo(ModInfo other, String name, String description) {
        this(
                other.fileId,
                other.fileName,
                other.version,
                other.enabled,
                other.size,
                other.sha1,
                other.curseProject,
                other.curseFile,
                name,
                description
        );
    }

}
