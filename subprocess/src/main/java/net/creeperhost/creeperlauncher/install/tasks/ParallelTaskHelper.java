package net.creeperhost.creeperlauncher.install.tasks;

import net.covers1624.quack.collection.StreamableIterable;
import net.creeperhost.creeperlauncher.pack.CancellationToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static net.covers1624.quack.util.SneakyUtils.notPossible;

/**
 * Created by covers1624 on 15/12/21.
 */
public class ParallelTaskHelper {

    private static final Logger LOGGER = LogManager.getLogger();

    public static <T extends Task<?>> void executeInParallel(@Nullable CancellationToken token, ExecutorService executor, List<T> tasks, @Nullable TaskProgressAggregator listener) throws Throwable {
        List<Throwable> failures = Collections.synchronizedList(new LinkedList<>());
        List<CompletableFuture<?>> futures = new LinkedList<>();
        for (Task<?> task : tasks) {
            CompletableFuture<?> future = CompletableFuture.runAsync(() -> {
                if (token != null && token.isCanceled()) return;
                try {
                    task.execute(token, listener);
                } catch (Throwable e) {
                    failures.add(e);
                }
            }, executor);
            futures.add(future);
        }
        for (CompletableFuture<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException ignored) {
                if (token != null && token.isCanceled()) {
                    for (CompletableFuture<?> f2 : futures) {
                        f2.cancel(true);
                    }
                }
            } catch (ExecutionException e) {
                LOGGER.error("Failed to execute task.", e);
            }
        }
        if (failures.isEmpty()) {
            return;
        }
        throw StreamableIterable.of(failures)
                .fold((a, b) -> {
                    a.addSuppressed(b);
                    return a;
                })
                .orElseThrow(notPossible());
    }

}
