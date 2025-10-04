package dev.ftb.app.data.mod;

import dev.ftb.app.data.modpack.ModpackVersionModsManifest;
import org.jetbrains.annotations.Nullable;

public record CurseMetadata(
        Type type,
        long curseProject,
        long curseFile,
        @Nullable String name,
        @Nullable String slug,
        @Nullable String synopsis,
        @Nullable String icon
) {

    public static CurseMetadata basic(long project, long file) {
        return new CurseMetadata(Type.BASIC, project, file, null, null, null, null);
    }

    public static CurseMetadata full(long project, long file, @Nullable String name, @Nullable String slug, @Nullable String synopsis, @Nullable String icon) {
        return new CurseMetadata(Type.FULL, project, file, name, slug, synopsis, icon);
    }
    
    public static CurseMetadata fromMod(ModpackVersionModsManifest.Mod mod) {
        return CurseMetadata.full(
            mod.getCurseProject(),
            mod.getCurseFile(),
            mod.getName(),
            mod.getCurseSlug(),
            mod.getSynopsis(),
            mod.getIcon()
        );
    }

    public enum Type {
        BASIC,
        FULL
    }
}
