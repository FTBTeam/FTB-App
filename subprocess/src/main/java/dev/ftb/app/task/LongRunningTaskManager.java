package dev.ftb.app.task;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.covers1624.quack.util.SneakyUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static net.covers1624.quack.util.SneakyUtils.unsafeCast;

public class LongRunningTaskManager {

    private static final Logger LOGGER = LogManager.getLogger();

    private final ExecutorService executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder()
            .setNameFormat("Long Running Executor %d")
            .setDaemon(true)
            .build()
    );
    private final List<LongRunningOperation> operations = new LinkedList<>();

    /**
     * Checks if any tasks of the given class filter are running.
     *
     * @param filter The filter.
     * @return If any existing tasks with this type are running.
     */
    public boolean anyTasksRunning(Class<?> filter) {
        synchronized (operations) {
            for (LongRunningOperation operation : operations) {
                return filter.isAssignableFrom(operation.getClass());
            }
        }
        return false;
    }

    /**
     * Get the running operation for the specified key.
     *
     * @param key The key.
     * @return The operation, returns null if no operation was found.
     */
    // TODO, this needs to move to having tasks return a 'key/id' when started, using that for queries.
    //  at the moment for simplicity this is written to be single type at a time
    @Nullable
    public <T> T getRunningOperation(Class<? extends T> key) {
        synchronized (operations) {
            for (LongRunningOperation operation : operations) {
                if (operation.getClass() == key) {
                    return unsafeCast(operation);
                }
            }
        }
        return null;
    }

    CompletableFuture<?> submit(LongRunningOperation op, Runnable r) {
        synchronized (operations) {
            LOGGER.info("Submitting LongRunningOp: {}", op);
            operations.add(op);
            // TODO protect onComplete from never being called if 'r' ever throws an exception.
            return CompletableFuture.runAsync(SneakyUtils.concat(r, () -> onComplete(op)), executor);
        }
    }

    private void onComplete(LongRunningOperation op) {
        synchronized (operations) {
            LOGGER.info("Completing LongRunningOp: {}", op);
            operations.remove(op);
        }
    }

}
