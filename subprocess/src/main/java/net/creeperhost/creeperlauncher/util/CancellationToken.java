package net.creeperhost.creeperlauncher.util;

import java.util.concurrent.CompletableFuture;

/**
 * Created by covers1624 on 27/12/21.
 */
public class CancellationToken {

    public static final CancellationToken NOP = new UnCancelable();

    private boolean isCanceled;

    public boolean isCanceled() {
        return isCanceled;
    }

    public void cancel(CompletableFuture<?> future) {
        cancel();
        future.cancel(true);
    }

    public void cancel(Thread thread) {
        cancel();
        thread.interrupt();
    }

    public void cancel() {
        isCanceled = true;
    }

    public void throwIfCancelled() {
        if (isCanceled()) {
            throw new Cancellation();
        }
    }

    public static class Cancellation extends RuntimeException {
    }

    // @formatter:off
    private static class UnCancelable extends CancellationToken {
        @Override public boolean isCanceled() { return false; }
        @Override public void cancel(CompletableFuture<?> future) { }
        @Override public void cancel(Thread thread) { }
        @Override public void cancel() { }
        @Override public void throwIfCancelled() { }
    }
    // @formatter:on
}
