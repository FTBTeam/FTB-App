package net.creeperhost.creeperlauncher.pack;

import java.util.concurrent.CompletableFuture;

/**
 * Created by covers1624 on 27/12/21.
 */
public class CancellationToken {

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
}
