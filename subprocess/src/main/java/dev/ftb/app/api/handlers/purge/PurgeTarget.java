package dev.ftb.app.api.handlers.purge;

import dev.ftb.app.AppPaths;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

public enum PurgeTarget {
    ALL(null),
    INSTANCE_CACHE((paths) -> List.of(
        paths.instancesDir().resolve(".localCache"),
        paths.binDir().resolve("support-meta.json"),
        paths.binDir().resolve("support-meta.json.etag")
    )),
    RUNTIMES((paths) -> List.of(
        paths.binDir().resolve("runtime")
    )),
    MINECRAFT((paths) -> List.of(
        paths.binDir().resolve("assets"),
        paths.binDir().resolve("libraries"),
        paths.binDir().resolve("versions")
    ));

    @Nullable
    private final Function<AppPaths, List<Path>> paths;

    PurgeTarget(@Nullable Function<AppPaths, List<Path>> paths) {
        this.paths = paths;
    }

    public Function<AppPaths, List<Path>> paths() {
        if (paths == null) {
            throw new UnsupportedOperationException("PurgeTarget " + this + " does not have paths defined.");
        }
        
        return paths;
    }
}
