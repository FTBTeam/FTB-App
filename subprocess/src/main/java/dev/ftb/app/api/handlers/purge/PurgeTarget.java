package dev.ftb.app.api.handlers.purge;

import com.google.common.base.Suppliers;
import dev.ftb.app.AppMain;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

public enum PurgeTarget {
    ALL(null),
    INSTANCE_CACHE(Suppliers.memoize(() -> List.of(
        AppMain.paths().instancesDir().resolve(".localCache"),
        AppMain.paths().binDir().resolve("support-meta.json"),
        AppMain.paths().binDir().resolve("support-meta.json.etag")
    ))),
    RUNTIMES(Suppliers.memoize(() -> List.of(
        AppMain.paths().binDir().resolve("runtime")
    ))),
    MINECRAFT(Suppliers.memoize(() -> List.of(
        AppMain.paths().binDir().resolve("assets"),
        AppMain.paths().binDir().resolve("libraries"),
        AppMain.paths().binDir().resolve("versions")
    )));

    @Nullable
    private final Supplier<List<Path>> paths;

    PurgeTarget(@Nullable Supplier<List<Path>> paths) {
        this.paths = paths;
    }

    public Supplier<List<Path>> paths() {
        if (paths == null) {
            throw new UnsupportedOperationException("PurgeTarget " + this + " does not have paths defined.");
        }
        
        return paths;
    }
}
