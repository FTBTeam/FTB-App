package dev.ftb.app.install.tasks;

import dev.ftb.app.pack.CancellationToken;
import org.jspecify.annotations.Nullable;

/**
 * A Task.
 */
public interface Task {
    /**
     * Execute this task.
     *
     * @param listener The listener to receive updates about this task.
     * @throws Throwable If any exception is thrown whilst executing the task.
     */
    void execute(@Nullable CancellationToken cancelToken, @Nullable TaskProgressListener listener) throws Throwable;
}
