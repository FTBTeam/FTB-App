package dev.ftb.app.api.handlers.purge;

import dev.ftb.app.AppMain;
import net.covers1624.quack.util.LazyValue;

import java.nio.file.Path;
import java.util.List;

public enum PurgeTarget {
    ALL(null),
    INSTANCE_CACHE(new LazyValue<>(() -> List.of(
        AppMain.paths().instancesDir().resolve(".localCache"),
        AppMain.paths().binDir().resolve("support-meta.json"),
        AppMain.paths().binDir().resolve("support-meta.json.etag")
    ))),
    RUNTIMES(new LazyValue<>(() -> List.of(
        AppMain.paths().binDir().resolve("runtime")
    ))),
    MINECRAFT(new LazyValue<>(() -> List.of(
        AppMain.paths().binDir().resolve("assets"),
        AppMain.paths().binDir().resolve("libraries"),
        AppMain.paths().binDir().resolve("versions")
    )));
    
    private final LazyValue<List<Path>> paths;

    PurgeTarget(LazyValue<List<Path>> paths) {
        this.paths = paths;
    }

    public LazyValue<List<Path>> paths() {
        if (paths == null) {
            throw new UnsupportedOperationException("PurgeTarget " + this + " does not have paths defined.");
        }
        
        return paths;
    }
}
