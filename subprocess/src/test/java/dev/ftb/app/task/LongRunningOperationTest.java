package dev.ftb.app.task;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by covers1624 on 3/6/22.
 */
@Disabled // Disabled as we don't need to actively test this functionality. It is pretty flaky and timing sensitive.
public class LongRunningOperationTest {

    @Test
    public void testSuccess() {
        LongRunningTaskManager taskManager = new LongRunningTaskManager();
        AtomicReference<CompletionReason> completion = new AtomicReference<>();
        AtomicReference<Throwable> exception = new AtomicReference<>();
        AtomicBoolean didOperation = new AtomicBoolean();
        LongRunningOperation operation = new LongRunningOperation(taskManager) {
            @Override
            protected void onComplete(CompletionReason reason) {
                completion.set(reason);
                synchronized (completion) {
                    completion.notifyAll();
                }
            }

            @Override
            protected void doOperation() {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    fail(e);
                }
                didOperation.set(true);
            }

            @Override
            protected void onOperationException(Throwable ex) {
                exception.set(ex);
            }
        };
        assertFalse(operation.wasStarted());
        assertFalse(operation.isComplete());
        operation.submit();
        assertTrue(taskManager.anyTasksRunning(LongRunningOperation.class));
        assertTrue(operation.wasStarted());

        synchronized (completion) {
            try {
                completion.wait();
            } catch (InterruptedException e) {
                fail(e);
            }
        }
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            fail(e);
        }
        assertNull(exception.get());
        assertEquals(CompletionReason.NORMAL, completion.get());
        assertTrue(didOperation.get());
        // If random errors popup here, its probably a race condition
        assertTrue(operation.isComplete());
        assertFalse(taskManager.anyTasksRunning(LongRunningOperation.class));
    }

    @Test
    public void testCancellation() {
        LongRunningTaskManager taskManager = new LongRunningTaskManager();
        AtomicReference<CompletionReason> completion = new AtomicReference<>();
        AtomicReference<Throwable> exception = new AtomicReference<>();
        AtomicBoolean didOperation = new AtomicBoolean();
        LongRunningOperation operation = new LongRunningOperation(taskManager) {
            @Override
            protected void onComplete(CompletionReason reason) {
                completion.set(reason);
                synchronized (completion) {
                    completion.notifyAll();
                }
            }

            @Override
            protected void doOperation() {
                didOperation.set(true);

                for (int i = 0; i < 2000; i++) {

                    if (i % 10 == 0) {
                        cancelToken.throwIfCancelled();
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        fail(e);
                    }
                }

            }

            @Override
            protected void onOperationException(Throwable ex) {
                exception.set(ex);
            }
        };
        assertFalse(operation.wasStarted());
        assertFalse(operation.isComplete());
        operation.submit();
        assertTrue(taskManager.anyTasksRunning(LongRunningOperation.class));
        assertTrue(operation.wasStarted());
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            fail(e);
        }

        operation.cancel();

        synchronized (completion) {
            try {
                completion.wait();
            } catch (InterruptedException e) {
                fail(e);
            }
        }
        assertNull(exception.get());
        assertEquals(CompletionReason.CANCELED, completion.get());
        assertTrue(didOperation.get());
        assertTrue(operation.isComplete());
        assertFalse(taskManager.anyTasksRunning(LongRunningOperation.class));
    }

    @Test
    public void testFailure() {
        LongRunningTaskManager taskManager = new LongRunningTaskManager();
        AtomicReference<CompletionReason> completion = new AtomicReference<>();
        AtomicReference<Throwable> exception = new AtomicReference<>();
        AtomicBoolean didOperation = new AtomicBoolean();
        LongRunningOperation operation = new LongRunningOperation(taskManager) {
            @Override
            protected void onComplete(CompletionReason reason) {
                completion.set(reason);
                synchronized (completion) {
                    completion.notifyAll();
                }
            }

            @Override
            protected void doOperation() {
                didOperation.set(true);

                for (int i = 0; i < 2000; i++) {

                    if (i == 5) {
                        throw new RuntimeException("Abort");
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        fail(e);
                    }
                }
            }

            @Override
            protected void onOperationException(Throwable ex) {
                exception.set(ex);
            }
        };
        assertFalse(operation.wasStarted());
        assertFalse(operation.isComplete());
        operation.submit();
        assertTrue(taskManager.anyTasksRunning(LongRunningOperation.class));
        assertTrue(operation.wasStarted());

        synchronized (completion) {
            try {
                completion.wait();
            } catch (InterruptedException e) {
                fail(e);
            }
        }
        assertNotNull(exception.get());
        assertEquals("Abort", exception.get().getMessage());
        assertEquals(CompletionReason.EXCEPTION, completion.get());
        assertTrue(didOperation.get());
        assertTrue(operation.isComplete());
        assertFalse(taskManager.anyTasksRunning(LongRunningOperation.class));
    }

    private static ThreadFactory daemonTestFactory(String name) {
        return new ThreadFactoryBuilder()
                .setNameFormat(name + "-Thread-%d")
                .setDaemon(true)
                .build();
    }
}
