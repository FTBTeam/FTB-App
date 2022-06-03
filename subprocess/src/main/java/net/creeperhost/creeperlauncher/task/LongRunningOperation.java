package net.creeperhost.creeperlauncher.task;

import net.covers1624.quack.util.SneakyUtils;
import net.creeperhost.creeperlauncher.pack.CancellationToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Created by covers1624 on 3/6/22.
 */
public abstract class LongRunningOperation {

    private static final Logger LOGGER = LogManager.getLogger();

    private final LongRunningTaskManager manager;
    protected final CancellationToken cancelToken = new CancellationToken();
    @Nullable
    private CompletableFuture<?> future;

    protected LongRunningOperation(LongRunningTaskManager manager) {
        this.manager = manager;
    }

    public final void cancel() {
        if (future == null) {
            throw new IllegalStateException("Operation not started.");
        }
        cancelToken.cancel();
        future.cancel(false);
    }

    public final void submit() {
        if (future != null) {
            throw new IllegalStateException("Operation already submitted.");
        }

        future = manager.submit(this, wrapOperation(this::doOperation));
    }

    public boolean wasStarted() {
        return future != null;
    }

    public boolean isComplete() {
        return future != null && future.isDone();
    }

    /**
     * Called to perform the operation.
     */
    protected abstract void doOperation() throws Throwable;

    /**
     * Called when an exception is thrown whilst this operation is executing.
     *
     * @param ex The exception.
     */
    protected abstract void onOperationException(Throwable ex);

    /**
     * Called when the operation is marked as complete.
     *
     * @param reason The reason for the completion.
     */
    protected abstract void onComplete(CompletionReason reason);

    private Runnable wrapOperation(SneakyUtils.ThrowingRunnable<Throwable> r) {
        return () -> {
            CompletionReason reason = CompletionReason.NORMAL;
            try {
                r.run();
            } catch (Throwable ex) {
                if (ex instanceof CancellationToken.Cancellation) {
                    reason = CompletionReason.CANCELED;
                } else {
                    onOperationException(ex);
                    reason = CompletionReason.EXCEPTION;
                }
            }
            try {
                onComplete(reason);
            } catch (Throwable ex) {
                LOGGER.error("Exception thrown whilst running Operation onComplete. This should probably not happen..", ex);
            }
        };
    }

}
